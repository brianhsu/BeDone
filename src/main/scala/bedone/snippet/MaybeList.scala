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

    private def projectMaybes = projectID.map(Maybe.findByProject(currentUser, _).openOr(Nil))
    private def topicMaybes = topicID.map(Maybe.findByTopic(currentUser, _).openOr(Nil))
    private def allMaybes = Maybe.findByUser(currentUser).openOr(Nil)

    private def maybes = (projectMaybes or topicMaybes).getOrElse(allMaybes)

    private lazy val currentUser = CurrentUser.get.get
    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentTabID: String = "maybeAllTab"

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
            case "maybeAllTab"     => this.maybes
            case "maybeTicklerTab" => this.maybes.filter(_.tickler.is.isDefined)
            case "maybeStaredTab"  => this.maybes.filter(_.stuff.isStared.is)
        }

        """$('.maybeTab li').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(tabID) &
        JqSetHtml("maybeList", maybes.filter(shouldDisplay).flatMap(createMaybeRow))
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
        JqSetHtml("maybeList", maybes.flatMap(createMaybeRow)) &
        JsRaw("""$('#maybeShowAll').prop("disabled", true)""") &
        JsRaw("""$('#maybeCurrent').attr("class", "btn btn-inverse")""") &
        updateList(currentTabID)
    }

    def editPostAction(stuff: Stuff): JsCmd = {
        updateList(currentTabID)
    }

    def showEditForm(maybe: Maybe) = 
    {
        val editStuff = new EditMaybeForm(maybe, editPostAction)

        """$('#maybeEdit').remove()""" &
        SetHtml("maybeEditHolder", editStuff.toForm)
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

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(maybe)) &
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

    def createMaybeRow(maybe: Maybe) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "maybe" :: "item" :: Nil)

        val stuff = maybe.stuff

        val cssBinding = 
            actionBar(maybe) &
            ".maybe [id]"   #> ("maybe" + stuff.idField) &
            ".collapse [id]" #> ("maybeDesc" + stuff.idField) &
            ".title *"       #> stuff.titleWithLink &
            ".desc *"        #> stuff.descriptionHTML &
            ".topic *"       #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"     #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".deadline"      #> "" &
            ".ticklerDate"   #> formatTickler(maybe) 

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def render = 
    {
        "#maybeAllTab [onclick]" #> SHtml.onEvent(s => updateList("maybeAllTab")) &
        "#maybeTicklerTab [onclick]" #> SHtml.onEvent(s => updateList("maybeTicklerTab")) &
        "#maybeStaredTab [onclick]" #> SHtml.onEvent(s => updateList("maybeStaredTab")) &
        "#maybeShowAll [onclick]" #> SHtml.onEvent(s => showAllStuff()) &
        "#maybeList" #> (".row" #> maybes.map(createMaybeRow))
    }
}
