package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds._

import net.liftweb.http.SHtml
import net.liftweb.http.Templates

import net.liftweb.util.ClearClearable

import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat

class DelegatedAction extends JSImplicit
{
    lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentTabID: String = "notInformed"

    val currentUser = CurrentUser.get.get

    def delegatedAction = Delegated.findByUser(currentUser)
                                   .openOr(Nil)
                                   .filterNot(_.action.stuff.isTrash.is)

    def notInformedAction = delegatedAction.filterNot(_.hasInformed.is)
    def notRespondAction = delegatedAction.filter(x => x.hasInformed.is && !x.action.isDone.is)
    def doneAction = delegatedAction.filter(_.action.isDone.is)

    def formatDoneTime(action: Action) = 
    {
        action.doneTime.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateTimeFormatter.format(calendar.getTime)
        }
    }

    def actionBar(delegated: Delegated) = 
    {
        val action = delegated.action
        val stuff = action.stuff

        def starClass = stuff.isStared.is match {
            case true  => "myicon-starOn"
            case false => "myicon-starOff"
        }

        def toogleStar(): JsCmd = 
        {
            stuff.isStared(!stuff.isStared.is)
            stuff.saveTheRecord()
            
            """$('#row%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def markAsTrash(): JsCmd = 
        {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            FadeOutAndRemove("row" + stuff.idField)
        }

        def markDoneFlag(isDone: Boolean): JsCmd = 
        {
            currentTabID match {
                case "notInformed" => 
                    delegated.hasInformed(true).saveTheRecord()
                case "notRespond" => 
                    delegated.hasInformed(true).saveTheRecord()
                    delegated.action.isDone(true).saveTheRecord()
                case "isDone" => 
                    delegated.hasInformed(false).saveTheRecord()
                    delegated.action.isDone(false).saveTheRecord()
            }

            FadeOutAndRemove("row" + stuff.idField)
        }

        // ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(scheduled)) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#desc" + stuff.idField) &
        ".isDone" #> SHtml.ajaxCheckbox(action.isDone.is, markDoneFlag _)
    }

    def topicFilter(buttonID: String, topic: Topic) = 
    {
        this.currentProject = None
        this.currentTopic = Some(topic)

        JqSetHtml("current", topic.title.is) &
        """$('#showAll').prop("disabled", false)""" &
        """$('#current').attr("class", "btn btn-info")""" &
        updateList(currentTabID)
    }

    def projectFilter(buttonID: String, project: Project) =
    {
        this.currentProject = Some(project)
        this.currentTopic = None

        JqSetHtml("current", project.title.is) &
        """$('#showAll').prop("disabled", false)""" &
        """$('#current').attr("class", "btn btn-success")""" &
        updateList(currentTabID)
    }

    def showAllStuff() = 
    {
        this.currentTopic = None
        this.currentProject = None

        updateList(currentTabID) &
        JqSetHtml("current", "全部") &
        """$('#showAll').prop("disabled", true)""" &
        """$('#current').attr("class", "btn btn-inverse")"""
    }

    def contactFilter(buttonID: String, contact: Contact) =
    {
        Noop
    }

    def shouldDisplay(delegated: Delegated) = 
    {
        val hasTopic = currentTopic.map(delegated.action.topics.contains).getOrElse(true)
        val hasProject = currentProject.map(delegated.action.projects.contains).getOrElse(true)

        hasTopic && hasProject
    }

    def updateList(tabID: String): JsCmd = 
    {
        this.currentTabID = tabID

        var events = tabID match {
            case "notInformed" => notInformedAction
            case "notRespond" => notRespondAction
            case "isDone" => doneAction
        }

        """$('.delegatedTab li').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(tabID) &
        JqSetHtml("eventList", events.filter(shouldDisplay).flatMap(createActionRow))
    }

    def createActionRow(delegated: Delegated) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "delegated" :: "item" :: Nil)

        val action = delegated.action
        val stuff = action.stuff

        val cssBinding = 
            actionBar(delegated) &
            ".action [id]"    #> ("row" + action.idField) &
            ".collapse [id]"  #> ("desc" + action.stuff.idField) &
            ".title *"        #> stuff.title &
            ".desc *"         #> stuff.descriptionHTML &
            ".topic *"        #> action.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"      #> action.projects.map(_.viewButton(projectFilter)).flatten &
            ".doneTime"       #> formatDoneTime(action) &
            ".contact"        #> delegated.contact.viewButton(contactFilter)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def render = 
    {
        println("==>" + delegatedAction)
        println("==>" + notInformedAction)

        ClearClearable &
        "#eventList *" #> notInformedAction.flatMap(createActionRow) &
        "#notInformed [onclick]" #> SHtml.onEvent(s => updateList("notInformed")) &
        "#notRespond [onclick]"  #> SHtml.onEvent(s => updateList("notRespond")) &
        "#isDone [onclick]"      #> SHtml.onEvent(s => updateList("isDone")) &
        "#showAll [onclick]" #> SHtml.onEvent(s => showAllStuff())
    }
}
