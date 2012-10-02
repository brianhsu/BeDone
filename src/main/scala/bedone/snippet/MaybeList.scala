package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common.Box
import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import java.text.SimpleDateFormat

class MaybeList extends JSImplicit
{
    private val projectID = S.attr("projectID").map(_.toInt)
    private val topicID = S.attr("topicID").map(_.toInt)

    private def projectMaybeTs = projectID.map(Maybe.findByProject(currentUser, _).openOr(Nil))
    private def topicMaybeTs = topicID.map(Maybe.findByTopic(currentUser, _).openOr(Nil))
    private def allMaybeTs = Maybe.findByUser(currentUser).openOr(Nil)

    private def maybeTs: List[MaybeT] = (projectMaybeTs or topicMaybeTs).getOrElse(allMaybeTs)

    private lazy val currentUser = CurrentUser.get.get
    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentTabID: String = "maybeAllTab"
    private var currentPage: Int = 1

    def shouldDisplay(maybeT: MaybeT) = 
    {
        val stuff = maybeT.stuff
        currentTopic.map(t => stuff.hasTopic(t.idField.is)).getOrElse(true) &&
        currentProject.map(p => stuff.hasProject(p.idField.is)).getOrElse(true)
    }
    
    def onSwitchPage(paging: Paging[MaybeT], page: Int): JsCmd = {
        currentPage = page
        updateList(currentTabID)
    }

    def getPagedMaybe(maybes: List[MaybeT]) = new Paging(maybes.toArray, 10, 5, onSwitchPage)

    def updateList(tabID: String) =
    {
        if (tabID != currentTabID) {
            currentPage = 1
        }

        this.currentTabID = tabID

        val maybeTs = tabID match {
            case "maybeAllTab"     => getPagedMaybe(this.maybeTs.filter(shouldDisplay))
            case "maybeTicklerTab" => getPagedMaybe(this.maybeTs.filter(_.maybe.tickler.is.isDefined).filter(shouldDisplay))
            case "maybeStaredTab"  => getPagedMaybe(this.maybeTs.filter(_.stuff.isStared.is).filter(shouldDisplay))
        }

        """$('.maybeTab li').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(tabID) &
        JqSetHtml("maybeList", maybeTs(currentPage).flatMap(createMaybeRow)) &
        JqSetHtml("maybePageSelector", maybeTs.pageSelector(currentPage))
    }

    def topicFilter(buttonID: String, topic: Topic): JsCmd = 
    {
        this.currentTopic = Some(topic)
        this.currentProject = None
        
        JqSetHtml("maybeCurrent", topic.title.is) &
        JsRaw("""$('#maybeShowAll').prop("disabled", false)""") &
        JsRaw("""$('#maybeCurrent').attr("class", "btn btn-info")""") &
        updateList(currentTabID)
    }

    def projectFilter(buttonID: String, project: Project): JsCmd = 
    {
        this.currentTopic = None
        this.currentProject = Some(project)

        JqSetHtml("maybeCurrent", project.title.is) &
        JsRaw("""$('#maybeShowAll').prop("disabled", false)""") &
        JsRaw("""$('#maybeCurrent').attr("class", "btn btn-success")""") &
        updateList(currentTabID)
    }

    def showAllStuff() = 
    {
        this.currentTopic = None
        this.currentProject = None

        JqSetHtml("maybeCurrent", "全部") &
        JqSetHtml("maybeList", maybeTs.flatMap(createMaybeRow)) &
        JsRaw("""$('#maybeShowAll').prop("disabled", true)""") &
        JsRaw("""$('#maybeCurrent').attr("class", "btn btn-inverse")""") &
        updateList(currentTabID)
    }

    def editPostAction(stuff: Stuff): JsCmd = {
        updateList(currentTabID)
    }

    def showEditForm(maybeT: MaybeT) = 
    {
        val editStuff = new EditMaybeForm(maybeT, editPostAction)

        """$('#maybeEdit').remove()""" &
        SetHtml("maybeEditHolder", editStuff.toForm)
    }

    def actionBar(maybeT: MaybeT) = 
    {
        val stuff = maybeT.stuff

        def starClass = stuff.isStared.is match {
            case true  => "myicon-starOn"
            case false => "myicon-starOff"
        }

        def toogleStar(): JsCmd = {
            
            stuff.isStared(!stuff.isStared.is)
            stuff.saveTheRecord()

            val fadeOutEffect = if (currentTabID == "maybeStaredTab") {
                FadeOutAndRemove("maybe" + stuff.idField) 
            } else {
                Noop
            }

            """$('#maybe%s .star i').attr('class', '%s')""".format(stuff.idField, starClass) &
            fadeOutEffect
        }

        def reInbox(): JsCmd = 
        {
            stuff.reInbox()
            FadeOutAndRemove("maybe" + stuff.idField.is)
        }

        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            new FadeOut("maybe" + stuff.idField, 0, 500)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(maybeT)) &
        ".reinbox [onclick]" #> SHtml.onEvent(s => reInbox) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#maybeDesc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility
    }

    def formatTickler(maybe: Maybe) = 
    {
        maybe.tickler.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateFormatter.format(calendar.getTime)
        }
    }

    def createMaybeRow(maybeT: MaybeT) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "maybe" :: "item" :: Nil)

        val stuff = maybeT.stuff

        val cssBinding = 
            actionBar(maybeT) &
            ".maybe [id]"   #> ("maybe" + stuff.idField) &
            ".collapse [id]" #> ("maybeDesc" + stuff.idField) &
            ".title *"       #> stuff.titleWithLink &
            ".desc *"        #> stuff.descriptionHTML &
            ".topic *"       #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"     #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".deadline"      #> "" &
            ".ticklerDate"   #> formatTickler(maybeT.maybe) 

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def render = 
    {
        val pagedMaybes = getPagedMaybe(maybeTs)
        "#maybeAllTab [onclick]" #> SHtml.onEvent(s => updateList("maybeAllTab")) &
        "#maybeTicklerTab [onclick]" #> SHtml.onEvent(s => updateList("maybeTicklerTab")) &
        "#maybeStaredTab [onclick]" #> SHtml.onEvent(s => updateList("maybeStaredTab")) &
        "#maybeShowAll [onclick]" #> SHtml.onEvent(s => showAllStuff()) &
        "#maybeList" #> (".row" #> pagedMaybes(currentPage).map(createMaybeRow)) &
        "#maybePageSelector *" #> pagedMaybes.pageSelector(currentPage)
    }
}
