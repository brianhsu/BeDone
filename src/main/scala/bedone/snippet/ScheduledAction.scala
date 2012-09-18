package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common.Box

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

import org.joda.time._

class ScheduledAction extends JSImplicit
{
    private lazy val currentUser = CurrentUser.get.get
    private lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentTabID: String = "scheduledWeekTab"

    private val projectID = S.attr("projectID").map(_.toInt)
    private val topicID = S.attr("topicID").map(_.toInt)

    def projectScheduled = projectID.map(Scheduled.findByProject(currentUser, _).openOr(Nil))
    def topicScheduled = topicID.map(Scheduled.findByTopic(currentUser, _).openOr(Nil))
    def userScheduled = Scheduled.findByUser(currentUser).openOr(Nil)
    def scheduledAction = (projectScheduled orElse topicScheduled).getOrElse(userScheduled)

    def todayAction = scheduledAction.filter(isToday)
    def weekAction  = scheduledAction.filter(isThisWeek).filterNot(isToday)
    def monthAction = scheduledAction.filter(isThisMonth).filterNot(isToday)
    def allAction = scheduledAction.filterNot(isToday)

    def isOutdated(scheduled: Scheduled): Boolean = 
    {
        val todayStart = (new DateMidnight())

        scheduled.startTime.is.getTime.before(todayStart.toDate) &&
        !scheduled.action.isDone.is
    }

    def isThisMonth(scheduled: Scheduled): Boolean =
    {
        val monthStart = (new DateMidnight).withDayOfMonth(1)
        val monthEnd = monthStart.plusMonths(1)

        scheduled.startTime.is.getTime.after(monthStart.toDate) &&
        scheduled.startTime.is.getTime.before(monthEnd.toDate)
    }

    def isThisWeek(scheduled: Scheduled): Boolean = 
    {
        val weekStart = (new DateMidnight).withDayOfWeek(1)
        val weekEnd = weekStart.plusDays(7)

        scheduled.startTime.is.getTime.after(weekStart.toDate) &&
        scheduled.startTime.is.getTime.before(weekEnd.toDate)
    }

    def isToday(scheduled: Scheduled): Boolean = 
    {
        val todayStart = (new DateMidnight())
        val todayEnd = (new DateMidnight()).plusDays(1)

        scheduled.startTime.is.getTime.after(todayStart.toDate) &&
        scheduled.startTime.is.getTime.before(todayEnd.toDate)
    }

    def formatDoneTime(action: Action) = 
    {
        action.doneTime.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateTimeFormatter.format(calendar.getTime)
        }
    }

    def formatStartTime(scheduled: Scheduled) = 
    {
        ".label *" #> dateTimeFormatter.format(scheduled.startTime.is.getTime)
    }

    def showAllStuff() = 
    {
        this.currentTopic = None
        this.currentProject = None

        updateList() &
        JqSetHtml("scheduledCurrent", "全部") &
        """$('#scheduledShowAll').prop("disabled", true)""" &
        """$('#scheduledCurrent').attr("class", "btn btn-inverse")"""
    }

    def topicFilter(buttonID: String, topic: Topic) = 
    {
        this.currentProject = None
        this.currentTopic = Some(topic)

        updateList() &
        JqSetHtml("scheduledCurrent", topic.title.is) &
        """$('#scheduledShowAll').prop("disabled", false)""" &
        """$('#scheduledCurrent').attr("class", "btn btn-info")"""
    }

    def projectFilter(buttonID: String, project: Project) =
    {
        this.currentProject = Some(project)
        this.currentTopic = None

        updateList() &
        JqSetHtml("scheduledCurrent", project.title.is) &
        """$('#scheduledShowAll').prop("disabled", false)""" &
        """$('#scheduledCurrent').attr("class", "btn btn-success")"""
    }

    def updateList(): JsCmd = updateList(this.currentTabID)

    def createTooltip(scheduled: Scheduled) = 
    {
        val endTime = scheduled.endTime.is.map { x => 
            "結束時間：<br>" + dateTimeFormatter.format(x.getTime) + "<br>"
        }
        val location = scheduled.location.is.map(x => "地點：<br>" + x)

        endTime.getOrElse("") + location.getOrElse("")
    }

    def createActionRow(scheduled: Scheduled) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "scheduled" :: "item" :: Nil)

        val action = scheduled.action
        val stuff = action.stuff

        val cssBinding = 
            actionBar(scheduled) &
            ".scheduled [id]"    #> ("scheduled" + action.idField) &
            ".collapse [id]"  #> ("scheduledDesc" + action.stuff.idField) &
            ".title *"        #> stuff.titleWithLink &
            ".desc *"         #> stuff.descriptionHTML &
            ".topic *"        #> action.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"      #> action.projects.map(_.viewButton(projectFilter)).flatten &
            ".startTime"      #> formatStartTime(scheduled) &
            ".doneTime"       #> formatDoneTime(action) &
            "rel=tooltip [title]" #> createTooltip(scheduled)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def shouldDisplay(scheduled: Scheduled) = 
    {
        val hasTopic = currentTopic.map(scheduled.action.topics.contains).getOrElse(true)
        val hasProject = currentProject.map(scheduled.action.projects.contains).getOrElse(true)
   
        hasTopic && hasProject
    }

    def createActionList(intervalAction: List[Scheduled]) = 
    {
        val outdatedList = allAction.filter(isOutdated).filter(shouldDisplay)
        val todayList = todayAction.filter(shouldDisplay).filterNot(isOutdated)
        val intervalList = intervalAction.filter(shouldDisplay).filterNot(isOutdated)
        val doneList = 
            todayList.filter(_.action.isDone.is) ++ 
            intervalList.filter(_.action.isDone.is)

        (todayList.filterNot(_.action.isDone.is), intervalList.filterNot(_.action.isDone.is), doneList, outdatedList)
    }

    def updateList(tabID: String): JsCmd =
    {
        this.currentTabID = tabID

        val title = tabID match {
            case "scheduledWeekTab"  => "本週"
            case "scheduledMonthTab" => "本月"
            case "scheduledAllTab" => "全部"
        }

        val intervalAction = tabID match {
            case "scheduledWeekTab"  => weekAction
            case "scheduledMonthTab" => monthAction
            case "scheduledAllTab" => allAction
        }

        updateList(tabID, title, intervalAction)
    }

    def updateList(tabID: String, title: String, intervalAction: List[Scheduled]): JsCmd = 
    {
        val (todayList, intervalList, doneList, outdatedList) = createActionList(intervalAction)

        """$('.intervalTab').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(tabID) &
        JqSetHtml("scheduledIntervalLabel", title)  &
        JqSetHtml("scheduledIntervalList", intervalList.flatMap(createActionRow)) &
        JqSetHtml("scheduledTodayList", todayList.flatMap(createActionRow)) &
        JqSetHtml("scheduledDoneList", doneList.flatMap(createActionRow)) &
        JqSetHtml("scheduledOutdatedList", outdatedList.flatMap(createActionRow)) &
        JqSetVisible("scheduledOutdatedBlock", !outdatedList.isEmpty) &
        JqSetVisible("scheduledIntervalBlock", !intervalList.isEmpty) &
        JqSetVisible("scheduledTodayBlock", !todayList.isEmpty) &
        JqSetVisible("scheduledDoneBlock", !doneList.isEmpty)
    }

    def editPostAction(stuff: Stuff): JsCmd = {
        updateList()
    }

    def showEditForm(scheduled: Scheduled) = 
    {
        val editStuff = new EditScheduledForm(scheduled, editPostAction)

        """$('#scheduledEdit').remove()""" &
        SetHtml("scheduledEditHolder", editStuff.toForm)
    }

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

            new FadeOut("scheduled" + stuff.idField, 0, 500)
        }

        def reInbox(): JsCmd = 
        {
            stuff.reInbox()
            FadeOutAndRemove("scheduled" + stuff.idField.is)
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

            updateList &
            Hide(rowID) &
            new FadeIn(rowID, 200, 2500)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(scheduled)) &
        ".reinbox [onclick]" #> SHtml.onEvent(s => reInbox) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#scheduledDesc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility &
        ".isDone" #> SHtml.ajaxCheckbox(action.isDone.is, markDoneFlag(action, _))
    }

    def render = 
    {
        val (todayList, intervalList, doneList, outdatedList) = createActionList(weekAction)
        val hidden = "display: none;";

        "#scheduledShowAll [onclick]" #> SHtml.onEvent(s => showAllStuff()) &
        "#scheduledWeekTab [onclick]" #> SHtml.onEvent(s => updateList("scheduledWeekTab")) &
        "#scheduledMonthTab [onclick]" #> SHtml.onEvent(s => updateList("scheduledMonthTab")) &
        "#scheduledAllTab [onclick]" #> SHtml.onEvent(s => updateList("scheduledAllTab")) &
        "#scheduledTodayList *"    #> todayList.flatMap(createActionRow) &
        "#scheduledIntervalList *" #> intervalList.flatMap(createActionRow) &
        "#scheduledDoneList *" #> doneList.flatMap(createActionRow) &
        "#scheduledOutdatedList *" #> outdatedList.flatMap(createActionRow) &
        "#scheduledOutdatedBlock [style+]" #> (if (outdatedList.isEmpty) hidden else "") &
        "#scheduledIntervalBlock [style+]" #> (if (intervalList.isEmpty) hidden else "") &
        "#scheduledTodayBlock [style+]" #> (if (todayList.isEmpty) hidden else "") &
        "#scheduledDoneBlock [style+]" #> (if (doneList.isEmpty) hidden else "")
    }
}
