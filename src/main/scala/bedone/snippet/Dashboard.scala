package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmd

import net.liftweb.util.Helpers._

import java.util.Calendar
import java.util.Date

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

    private val todayScheduled = scheduleds.filter(x => isToday(x) || isOutdated(x))

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
        ".reinbox" #> "" &
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

    def render = {
        "#inboxCount *" #> inboxCount &
        "#nextActionProgress *" #> "%d / %d".format(doneActions, totalActions) &
        "#nextActionBar [style]" #> ("width: " + doneActionsPercent + "%") &
        "#delegatedProgress *" #> "%d / %d".format(doneDelegated, totalDelegated) &
        "#delegatedBar [style]" #> ("width: " + doneDelegatedPercent + "%") &
        "#scheduledProgress *" #> "%d / %d".format(totalScheduled, doneScheduled) &
        "#scheduledBar [style]" #> ("width: " + doneScheduledPercent + "%") &
        "#todayScheduled *" #> todayScheduled.map(createActionRow)
    }
}
