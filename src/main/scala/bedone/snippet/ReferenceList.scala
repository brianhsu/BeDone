package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import java.text.SimpleDateFormat

class ReferenceList extends JSImplicit
{
    def references = CurrentUser.get.flatMap(Stuff.findReferenceByUser)
                                .openOr(Nil).filterNot(_.isTrash.is)

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentTabID: String = "allReference"

    def actionBar(stuff: Stuff) = 
    {
        def starClass = stuff.isStared.is match {
            case true  => "myicon-starOn"
            case false => "myicon-starOff"
        }

        def toogleStar(): JsCmd = {
            
            stuff.isStared(!stuff.isStared.is)
            stuff.saveTheRecord()

            val fadeOutEffect = currentTabID == "isStared" match {
                case true  => FadeOutAndRemove("row" + stuff.idField) 
                case false => Noop
            }

            """$('#row%s .star i').attr('class', '%s')""".format(stuff.idField, starClass) &
            fadeOutEffect
        }

        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            new FadeOut("row" + stuff.idField, 0, 500)
        }

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(stuff)) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#desc" + stuff.idField)
    }

    def shouldDisplay(stuff: Stuff) = 
    {
        val hasTopic = currentTopic.map(stuff.topics.contains).getOrElse(true)
        val hasProject = currentProject.map(stuff.projects.contains).getOrElse(true)

        hasTopic && hasProject
    }

    def updateList(tabID: String) =
    {
        this.currentTabID = tabID

        val references = tabID match {
            case "allReference" => this.references
            case "isStared"     => this.references.filter(_.isStared.is)
        }

        """$('.referenceTab li').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(tabID) &
        JqSetHtml("referenceList", references.filter(shouldDisplay).flatMap(createStuffRow))
    }

    def topicFilter(buttonID: String, topic: Topic): JsCmd = 
    {
        this.currentTopic = Some(topic)
        this.currentProject = None
        
        JqSetHtml("current", topic.title.is) &
        JsRaw("""$('#showAll').prop("disabled", false)""") &
        JsRaw("""$('#current').attr("class", "btn btn-info")""") &
        updateList(currentTabID)
    }

    def projectFilter(buttonID: String, project: Project): JsCmd = 
    {
        this.currentTopic = None
        this.currentProject = Some(project)

        JqSetHtml("current", project.title.is) &
        JsRaw("""$('#showAll').prop("disabled", false)""") &
        JsRaw("""$('#current').attr("class", "btn btn-success")""") &
        updateList(currentTabID)
    }

    def showAllStuff() = 
    {
        this.currentTopic = None
        this.currentProject = None

        JqSetHtml("current", "全部") &
        JqSetHtml("referenceList", references.flatMap(createStuffRow)) &
        JsRaw("""$('#showAll').prop("disabled", true)""") &
        JsRaw("""$('#current').attr("class", "btn btn-inverse")""") &
        updateList(currentTabID)
    }

    def showEditForm(stuff: Stuff): JsCmd = 
    {
        val editStuff = new EditStuffForm(stuff, editPostAction _)

        """$('#stuffEdit').remove()""" &
        AppendHtml("editForm", editStuff.toForm) &
        """prepareStuffEditForm()"""
    }

    def editPostAction(stuff: Stuff): JsCmd = 
    {
        val newRow = createStuffRow(stuff).flatMap(_.child)
        JqSetHtml("row" + stuff.idField.is, newRow)
    }

    def createStuffRow(stuff: Stuff) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "stuff" :: "item" :: Nil)

        val cssBinding = 
            actionBar(stuff) &
            ".stuffs [id]"   #> ("row" + stuff.idField) &
            ".collapse [id]" #> ("desc" + stuff.idField) &
            ".title *"       #> stuff.title &
            ".desc *"        #> stuff.descriptionHTML &
            ".topic *"       #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"     #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".deadline"      #> ""

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def render = 
    {
        "#allReference [onclick]" #> SHtml.onEvent(s => updateList("allReference")) &
        "#isStared [onclick]" #> SHtml.onEvent(s => updateList("isStared")) &
        "#showAll [onclick]" #> SHtml.onEvent(s => showAllStuff()) &
        "#referenceList *" #> references.flatMap(createStuffRow)
    }
}
