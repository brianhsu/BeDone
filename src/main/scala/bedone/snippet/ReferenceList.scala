package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common.Box

import net.liftweb.util.Helpers._
import net.liftweb.util.CssSel

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import java.text.SimpleDateFormat

class ReferenceList extends JSImplicit
{
    private val projectID = S.attr("projectID").map(_.toInt)
    private val topicID = S.attr("topicID").map(_.toInt)

    private val currentUser = CurrentUser.get.get

    def allReferences = Stuff.findByUser(currentUser, StuffType.Reference)
    def projectReferences = projectID.map { projectID => 
        Stuff.findByProject(currentUser, projectID, StuffType.Reference)
    }

    def topicReferences = topicID.map { topicID =>
        Stuff.findByTopic(currentUser, topicID, StuffType.Reference)
    }

    def references = 
        (projectReferences orElse topicReferences).getOrElse(allReferences)
                                                  .openOr(Nil)
                                                  .filterNot(_.isTrash.is)

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentTabID: String = "referenceAllTab"
    private var currentPage: Int = 1

    def actionBar(stuff: Stuff): CssSel = 
    {
        def starClass = stuff.isStared.is match {
            case true  => "myicon-starOn"
            case false => "myicon-starOff"
        }

        def toogleStar(): JsCmd = {
            
            stuff.isStared(!stuff.isStared.is)
            stuff.saveTheRecord()

            val fadeOutEffect = currentTabID == "referenceStaredTab" match {
                case true  => FadeOutAndRemove("reference" + stuff.idField) 
                case false => Noop
            }

            """$('#reference%s .star i').attr('class', '%s')""".format(stuff.idField, starClass) &
            fadeOutEffect &
            updateList(currentTabID)
        }

        def reInbox(): JsCmd = 
        {
            stuff.reInbox()
            FadeOutAndRemove("reference" + stuff.idField.is) &
            updateList(currentTabID)
        }

        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            new FadeOut("reference" + stuff.idField, 0, 500) &
            updateList(currentTabID)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(stuff)) &
        ".reinbox [onclick]" #> SHtml.onEvent(s => reInbox) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#referenceDesc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility
    }

    def shouldDisplay(stuff: Stuff) = 
    {
        val hasTopic = currentTopic.map(stuff.topics.contains).getOrElse(true)
        val hasProject = currentProject.map(stuff.projects.contains).getOrElse(true)

        hasTopic && hasProject
    }

    def onSwitchPage(paging: Paging[Stuff], page: Int): JsCmd = {
        currentPage = page
        updateList(currentTabID)
    }

    def pagedReference(reference: List[Stuff]) = {
        new Paging(reference.toArray, 10, 10, onSwitchPage _)
    }

    def updateList(tabID: String) =
    {
        if (this.currentTabID != tabID) {
            currentPage = 1
        }

        this.currentTabID = tabID

        val references = tabID match {
            case "referenceAllTab"    => pagedReference(this.references.filter(shouldDisplay))
            case "referenceStaredTab" => pagedReference(this.references.filter(_.isStared.is).filter(shouldDisplay))
        }

        """$('.referenceTab li').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(tabID) &
        JqSetHtml("referenceList", references(currentPage).flatMap(createStuffRow)) &
        JqSetHtml("referencePageSelector", references.pageSelector(currentPage))
    }

    def topicFilter(buttonID: String, topic: Topic): JsCmd = 
    {
        this.currentTopic = Some(topic)
        this.currentProject = None
        
        JqSetHtml("referenceCurrent", topic.title.is) &
        JsRaw("""$('#referenceShowAll').prop("disabled", false)""") &
        JsRaw("""$('#referenceCurrent').attr("class", "btn btn-info")""") &
        updateList(currentTabID)
    }

    def projectFilter(buttonID: String, project: Project): JsCmd = 
    {
        this.currentTopic = None
        this.currentProject = Some(project)

        JqSetHtml("referenceCurrent", project.title.is) &
        JsRaw("""$('#referenceShowAll').prop("disabled", false)""") &
        JsRaw("""$('#referenceCurrent').attr("class", "btn btn-success")""") &
        updateList(currentTabID)
    }

    def showAllStuff() = 
    {
        this.currentTopic = None
        this.currentProject = None

        JqSetHtml("referenceCurrent", "全部") &
        JqSetHtml("referenceList", references.flatMap(createStuffRow)) &
        JsRaw("""$('#referenceShowAll').prop("disabled", true)""") &
        JsRaw("""$('#referenceCurrent').attr("class", "btn btn-inverse")""") &
        updateList(currentTabID)
    }

    def showEditForm(stuff: Stuff): JsCmd = 
    {
        val editStuff = new EditReferenceForm(stuff, editPostAction _)

        """$('#referenceEdit').remove()""" &
        SetHtml("referenceEditHolder", editStuff.toForm)
    }

    def editPostAction(stuff: Stuff): JsCmd = 
    {
        val newRow = createStuffRow(stuff).flatMap(_.child)
        JqSetHtml("reference" + stuff.idField.is, newRow)
    }

    def createStuffRow(stuff: Stuff) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "reference" :: "item" :: Nil)

        val cssBinding = 
            actionBar(stuff) &
            ".reference [id]" #> ("reference" + stuff.idField) &
            ".collapse [id]"  #> ("referenceDesc" + stuff.idField) &
            ".title *"        #> stuff.titleWithLink &
            ".desc *"         #> stuff.descriptionHTML &
            ".topic *"        #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"      #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".deadline"       #> ""

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def render = 
    {
        val paged = pagedReference(references)

        "#referenceAllTab [onclick]" #> SHtml.onEvent(s => updateList("referenceAllTab")) &
        "#referenceStaredTab [onclick]" #> SHtml.onEvent(s => updateList("referenceStaredTab")) &
        "#referenceShowAll [onclick]" #> SHtml.onEvent(s => showAllStuff()) &
        "#referenceList" #> (".row" #> paged(currentPage).map(createStuffRow)) &
        "#referencePageSelector *" #> paged.pageSelector(currentPage)
    }
}
