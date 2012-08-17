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

class MaybeList extends JSImplicit
{
    private def maybes = Maybe.findByUser(currentUser).openOr(Nil)
                              .filterNot(_.stuff.isTrash.is)

    private lazy val currentUser = CurrentUser.get.get
    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentTabID: String = "allMaybe"

    def shouldDisplay(maybe: Maybe) = 
    {
        val hasTopic = currentTopic.map(maybe.stuff.topics.contains).getOrElse(true)
        val hasProject = currentProject.map(maybe.stuff.projects.contains).getOrElse(true)

        hasTopic && hasProject
    }

    def updateList(tabID: String) =
    {
        this.currentTabID = tabID

        val maybes = tabID match {
            case "allMaybe"     => this.maybes
            case "hasTickler"   => this.maybes.filter(_.tickler.is.isDefined)
            case "isStared"     => this.maybes.filter(_.stuff.isStared.is)
        }

        """$('.maybeTab li').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(tabID) &
        JqSetHtml("maybeList", maybes.filter(shouldDisplay).flatMap(createMaybeRow))
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
        JqSetHtml("maybeList", maybes.flatMap(createMaybeRow)) &
        JsRaw("""$('#showAll').prop("disabled", true)""") &
        JsRaw("""$('#current').attr("class", "btn btn-inverse")""") &
        updateList(currentTabID)
    }

    def editPostAction(stuff: Stuff): JsCmd = {
        updateList(currentTabID)
    }

    def showEditForm(maybe: Maybe) = 
    {
        val editStuff = new EditMaybeForm(maybe, editPostAction)

        """$('#stuffEdit').remove()""" &
        AppendHtml("editForm", editStuff.toForm) &
        """prepareStuffEditForm()"""
    }


    def actionBar(maybe: Maybe) = 
    {
        val stuff = maybe.stuff

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

            println("fadeOutEffect:" + fadeOutEffect)

            """$('#row%s .star i').attr('class', '%s')""".format(stuff.idField, starClass) &
            fadeOutEffect
        }

        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            new FadeOut("row" + stuff.idField, 0, 500)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(maybe)) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#desc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility
    }

    def formatTickler(maybe: Maybe) = 
    {
        maybe.tickler.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateFormatter.format(calendar.getTime)
        }
    }

    def createMaybeRow(maybe: Maybe) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "maybe" :: "item" :: Nil)

        val stuff = maybe.stuff

        val cssBinding = 
            actionBar(maybe) &
            ".action [id]"   #> ("row" + stuff.idField) &
            ".collapse [id]" #> ("desc" + stuff.idField) &
            ".title *"       #> stuff.title &
            ".desc *"        #> stuff.descriptionHTML &
            ".topic *"       #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"     #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".deadline"      #> "" &
            ".ticklerDate"   #> formatTickler(maybe) 

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def render = 
    {
        "#allMaybe [onclick]" #> SHtml.onEvent(s => updateList("allMaybe")) &
        "#hasTickler [onclick]" #> SHtml.onEvent(s => updateList("hasTickler")) &
        "#isStared [onclick]" #> SHtml.onEvent(s => updateList("isStared")) &
        "#showAll [onclick]" #> SHtml.onEvent(s => showAllStuff()) &
        "#maybeList *" #> maybes.flatMap(createMaybeRow)
    }
}
