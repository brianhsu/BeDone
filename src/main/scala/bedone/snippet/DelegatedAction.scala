package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common.Box
import net.liftweb.common.Full

import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates

import net.liftweb.util.ClearClearable

import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat

class DelegatedAction extends JSImplicit
{
    lazy val contactID = S.attr("contactID").flatMap(x => tryo(x.toInt)).toOption
    lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentTabID: String = "delegateInform"
    private var currentPage: Int = 1

    private val projectID: Box[Int] = S.attr("projectID").map(_.toInt)
    private val topicID: Box[Int] = S.attr("topicID").map(_.toInt)

    val currentUser = CurrentUser.get.get

    def userDelegated = Delegated.findByUser(currentUser).openOr(Nil)
    def projectDelegated = projectID.map(Delegated.findByProject(currentUser, _).openOr(Nil))
    def topicDelegated = topicID.map(Delegated.findByTopic(currentUser, _).openOr(Nil))

    def allDelegatedAction = {
        (projectDelegated or topicDelegated).getOrElse(userDelegated)
    }

    def delegatedAction = contactID match {
        case None => allDelegatedAction
        case Some(id) => allDelegatedAction.filter(_.delegated.contactID.is == id)
    }

    def onSwitchPage(paging: Paging[DelegatedT], page: Int) = {
        currentPage = page
        updateList(currentTabID)
    }

    def notInformedAction = {
        val actions = delegatedAction.filterNot(_.delegated.hasInformed.is)
                                     .filter(shouldDisplay)

        new Paging(actions.toArray, 10, 10, onSwitchPage _)
    }

    def notRespondAction = {
        val actions = delegatedAction.filter { x => 
            x.delegated.hasInformed.is && !x.action.isDone.is
        }.filter(shouldDisplay)

        new Paging(actions.toArray, 10, 10, onSwitchPage _)
    }

    def doneAction = {
        val actions = delegatedAction.filter(_.action.isDone.is).filter(shouldDisplay)
        new Paging(actions.toArray, 10, 10, onSwitchPage _)
    }

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
            
            """$('#delegate%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def markAsTrash(): JsCmd = 
        {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            FadeOutAndRemove("delegate" + stuff.idField) &
            updateList(currentTabID)
        }

        def reInbox(): JsCmd = 
        {
            stuff.reInbox()
            FadeOutAndRemove("delegate" + stuff.idField.is) &
            updateList(currentTabID)
        }

        def markDoneFlag(isDone: Boolean): JsCmd = 
        {
            currentTabID match {
                case "delegateInform" => 
                    delegated.hasInformed(true).saveTheRecord()
                case "delegateResponse" => 
                    delegated.hasInformed(true).saveTheRecord()
                    delegated.action.isDone(true).doneTime(Calendar.getInstance).saveTheRecord()
                case "delegateDone" => 
                    delegated.hasInformed(false).saveTheRecord()
                    delegated.action.isDone(false).doneTime(None).saveTheRecord()
            }

            FadeOutAndRemove("delegate" + stuff.idField)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(delegated)) &
        ".reinbox [onclick]" #> SHtml.onEvent(s => reInbox) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#delegateDesc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility &
        ".isDone" #> SHtml.ajaxCheckbox(action.isDone.is, markDoneFlag _)
    }

    def editPostAction(stuff: Stuff): JsCmd = 
    {
        updateList(currentTabID)
    }

    def showEditForm(delegated: Delegated) = 
    {
        val editStuff = new EditDelegatedForm(currentUser, delegated, editPostAction)

        """$('#delegateEdit').remove()""" &
        SetHtml("delegateEditHolder", editStuff.toForm)
    }

    def topicFilter(buttonID: String, topic: Topic) = 
    {
        this.currentProject = None
        this.currentTopic = Some(topic)

        JqSetHtml("delegateCurrent", topic.title.is) &
        """$('#delegateShowAll').prop("disabled", false)""" &
        """$('#delegateCurrent').attr("class", "btn btn-info")""" &
        updateList(currentTabID)
    }

    def projectFilter(buttonID: String, project: Project) =
    {
        this.currentProject = Some(project)
        this.currentTopic = None

        JqSetHtml("delegateCurrent", project.title.is) &
        """$('#delegateShowAll').prop("disabled", false)""" &
        """$('#delegateCurrent').attr("class", "btn btn-success")""" &
        updateList(currentTabID)
    }

    def showAllStuff() = 
    {
        this.currentTopic = None
        this.currentProject = None

        updateList(currentTabID) &
        JqSetHtml("delegateCurrent", "全部") &
        """$('#delegateShowAll').prop("disabled", true)""" &
        """$('#delegateCurrent').attr("class", "btn btn-inverse")"""
    }

    def contactFilter(buttonID: String, contact: Contact) =
    {
        S.redirectTo("/contact/" + contact.idField.is)
    }

    def shouldDisplay(delegatedT: DelegatedT) = 
    {
        val stuff = delegatedT.stuff
        val hasTopic = currentTopic.map(t => stuff.hasTopic(t.idField.is)).getOrElse(true)
        val hasProject = currentProject.map(p => stuff.hasProject(p.idField.is)).getOrElse(true)

        hasTopic && hasProject
    }

    def updateList(tabID: String): JsCmd = 
    {
        if (currentTabID != tabID) {
            currentPage = 1
        }

        this.currentTabID = tabID

        var events = tabID match {
            case "delegateInform" => notInformedAction
            case "delegateResponse" => notRespondAction
            case "delegateDone" => doneAction
        }

        """$('.delegatedTab li').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(tabID) &
        JqSetHtml("delegateActions", events(currentPage).flatMap(createActionRow)) &
        JqSetHtml("delegatePaging", events.pageSelector(currentPage))
    }

    def createActionRow(delegatedT: DelegatedT) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "delegated" :: "item" :: Nil)

        val delegated = delegatedT.delegated
        val action = delegatedT.action
        val stuff = delegatedT.stuff

        val cssBinding = 
            actionBar(delegated) &
            ".delegate [id]"    #> ("delegate" + action.idField) &
            ".collapse [id]"  #> ("delegateDesc" + action.stuff.idField) &
            ".title *"        #> stuff.title.is &
            ".desc *"         #> stuff.descriptionHTML &
            ".topic *"        #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"      #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".doneTime"       #> formatDoneTime(action) &
            ".contact"        #> delegated.contact.viewButton(contactFilter)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def render = 
    {
        val actions = notInformedAction
        ClearClearable &
        "#delegateActions" #> (".row" #> actions(currentPage).map(createActionRow)) &
        "#delegateInform [onclick]" #> SHtml.onEvent(s => updateList("delegateInform")) &
        "#delegateResponse [onclick]"  #> SHtml.onEvent(s => updateList("delegateResponse")) &
        "#delegateDone [onclick]"      #> SHtml.onEvent(s => updateList("delegateDone")) &
        "#delegateShowAll [onclick]" #> SHtml.onEvent(s => showAllStuff()) &
        "#delegatePaging *" #> actions.pageSelector(currentPage)
    }
}
