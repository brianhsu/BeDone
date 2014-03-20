package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd

import net.liftweb.util.Helpers._

import java.util.Calendar
import java.util.Date

object DashboardSchedule extends ScheduledPredicate with JSImplicit
{
    def actionBar(scheduled: Scheduled) = 
    {
        val action = scheduled.action
        val stuff = action.stuff

        def starClass = stuff.isStared.is match {
            case true  => "myicon-starOn"
            case false => "myicon-starOff"
        }

        def toogleStar(): JsCmd = {
            stuff.isStared(!stuff.isStared.is)
            stuff.saveTheRecord()
            
            """$('#scheduled%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def reInbox(): JsCmd = 
        {
            stuff.reInbox()
            FadeOutAndRemove("scheduled" + stuff.idField)
        }


        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            FadeOutAndRemove("scheduled" + stuff.idField)
        }

        def markDoneFlag(action: Action, isDone: Boolean): JsCmd = 
        {
            val rowID = "scheduled" + action.idField.is
            val doneTime = isDone match {
                case false => None
                case true  =>
                    val calendar = Calendar.getInstance
                    calendar.setTime(new Date)
                    Some(calendar)
            }

            action.isDone(isDone)
            action.doneTime(doneTime)
            action.saveTheRecord()

            FadeOutAndRemove("scheduled" + stuff.idField)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".edit" #> "" &
        ".reinbox [onclick]" #> SHtml.onEvent(s => reInbox) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#scheduledDesc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility &
        ".isDone" #> SHtml.ajaxCheckbox(action.isDone.is, markDoneFlag(action, _))
    }

    def createActionRow(scheduledT: ScheduledT) = 
    {
        def template = Templates("templates-hidden" :: "scheduled" :: "item" :: Nil)

        val scheduled = scheduledT.scheduled
        val action = scheduledT.action
        val stuff = scheduledT.stuff

        val cssBinding = 
            actionBar(scheduled) &
            ".scheduled [id]" #> ("scheduled" + action.idField) &
            ".collapse [id]"  #> ("scheduledDesc" + action.stuff.idField) &
            ".title *"        #> stuff.titleWithLink &
            ".desc *"         #> stuff.descriptionHTML &
            ".topic *"        #> "" &
            ".project *"      #> "" &
            ".startTime"      #> formatStartTime(scheduled) &
            ".doneTime"       #> formatDoneTime(action) &
            "rel=tooltip [title]" #> createTooltip(scheduled)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

}

trait Filters {
    def topicFilter(buttonID: String, topic: Topic) = { S.redirectTo(s"/topic/${topic.id}") }
    def projectFilter(buttonID: String, project: Project) = { S.redirectTo(s"/project/${project.id}") }
    def contactFilter(buttonID: String, contact: Contact) = { S.redirectTo("/contact/" + contact.idField.is) }
}

object DashboardNextAction extends JSImplicit with Filters
{
    def actionBar(action: Action) = 
    {
        val stuff = action.stuff

        def starClass = stuff.isStared.is match {
            case true  => "myicon-starOn"
            case false => "myicon-starOff"
        }

        def reInbox(): JsCmd = 
        {
            stuff.reInbox()
            FadeOutAndRemove("action" + stuff.idField)
        }

        def toogleStar(): JsCmd = {
            stuff.isStared(!stuff.isStared.is)
            stuff.saveTheRecord()
            
            """$('#action%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.saveTheRecord()
            FadeOutAndRemove("action" + stuff.idField)
        }

        def markDoneFlag(action: Action, isDone: Boolean): JsCmd = 
        {
            val rowID = "action" + action.idField.is
            val doneTime = isDone match {
                case false => None
                case true  =>
                    val calendar = Calendar.getInstance
                    calendar.setTime(new Date)
                    Some(calendar)
            }

            action.isDone(isDone)
            action.doneTime(doneTime)
            action.saveTheRecord()

            FadeOutAndRemove("action" + stuff.idField)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".edit" #> "" &
        ".reinbox [onclick]" #> SHtml.onEvent(s => reInbox) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) & 
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#actionDesc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility &
        ".isDone" #> SHtml.ajaxCheckbox(action.isDone.is, markDoneFlag(action, _))
    }


    def createActionRow(actionT: ActionT) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "action" :: "item" :: Nil)

        val action = actionT.action
        val stuff = action.stuff

        val doneTimeFormatter = 
            action.formatDoneTime.map(dateTime => ".label *" #> dateTime).getOrElse("*" #> "")

        val deadlineFormatter = 
            action.formatDeadline.map(dateTime => ".label *" #> dateTime).getOrElse("*" #> "")

        val cssBinding = 
            actionBar(action) &
            ".action [id]"    #> ("action" + action.idField) &
            ".collapse [id]"  #> ("actionDesc" + action.stuff.idField) &
            ".title *"        #> stuff.titleWithLink &
            ".desc *"         #> stuff.descriptionHTML &
            ".topic *"        #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"      #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".deadline"       #> deadlineFormatter &
            ".doneTime"       #> doneTimeFormatter &
            ".listRow [data-stuffID]" #> stuff.idField.is

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

}

object DashboardDelegated extends JSImplicit with Filters
{
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
            FadeOutAndRemove("delegate" + stuff.idField)
        }

        def reInbox(): JsCmd = 
        {
            stuff.reInbox()
            FadeOutAndRemove("delegate" + stuff.idField)
        }

        def markDoneFlag(isDone: Boolean): JsCmd = 
        {
            delegated.action.isDone(false).doneTime(None).saveTheRecord()
            FadeOutAndRemove("delegate" + stuff.idField)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".edit" #> "" &
        ".reinbox [onclick]" #> SHtml.onEvent(s => reInbox) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#delegateDesc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility &
        ".isDone" #> SHtml.ajaxCheckbox(action.isDone.is, markDoneFlag _)
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
            ".doneTime"       #> "" &
            ".topic *"        #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"      #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".contact"        #> delegated.contact.viewButton(contactFilter)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

}

class DashBoard extends ScheduledPredicate with JSImplicit
{
    val currentUser = CurrentUser.get.get

    private val inboxCount = Stuff.findByUser(currentUser).openOr(Nil).size

    private val actions = Action.findByUser(currentUser).openOr(Nil)
    private val totalActions = actions.size
    private val doneActions = actions.filter(_.action.isDone.is).size
    private val doneActionsPercent = totalActions match {
        case 0     => 0
        case total => (doneActions.toDouble / total) * 100
    }

    private val delegateds = Delegated.findByUser(currentUser).openOr(Nil)
    private val totalDelegated = delegateds.size
    private val doneDelegated = delegateds.filter(_.action.isDone.is).size
    private val doneDelegatedPercent = totalDelegated match {
        case 0     => 0
        case total => (doneDelegated.toDouble / total) * 100
    }

    private val scheduleds = Scheduled.findByUser(currentUser).openOr(Nil)
    private val totalScheduled = scheduleds.size
    private val doneScheduled = scheduleds.filter(_.action.isDone.is).size
    private val doneScheduledPercent = totalScheduled match {
        case 0     => 0
        case total => (doneScheduled.toDouble / total) * 100
    }

    private val todayScheduled = scheduleds.filter(x => (isToday(x) || isOutdated(x)) && !isDone(x))

    def render = {
        val nextActions = actions.filterNot(_.action.isDone.is)
        val delegatedActions = delegateds.filterNot(_.action.isDone.is)

        "#inboxCount *" #> inboxCount &
        "#nextActionProgress *" #> "%d / %d".format(doneActions, totalActions) &
        "#nextActionBar [style]" #> ("width: " + doneActionsPercent + "%") &
        "#delegatedProgress *" #> "%d / %d".format(doneDelegated, totalDelegated) &
        "#delegatedBar [style]" #> ("width: " + doneDelegatedPercent + "%") &
        "#scheduledProgress *" #> "%d / %d".format(totalScheduled, doneScheduled) &
        "#scheduledBar [style]" #> ("width: " + doneScheduledPercent + "%") &
        "#todayScheduled *" #> todayScheduled.map(DashboardSchedule.createActionRow) &
        "#nextAction *" #> nextActions.map(DashboardNextAction.createActionRow) &
        "#delegated *" #> delegatedActions.map(DashboardDelegated.createActionRow)
    }
}
