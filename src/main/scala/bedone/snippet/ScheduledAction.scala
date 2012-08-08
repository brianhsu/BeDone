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

import org.joda.time._

class ScheduledAction extends JSImplicit
{
    lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentTabID: String = "thisWeekTab"

    val currentUser = CurrentUser.get.get


    def scheduledAction = Scheduled.findByUser(currentUser).openOr(Nil)
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
        JqSetHtml("current", "全部") &
        """$('#showAll').prop("disabled", true)""" &
        """$('#current').attr("class", "btn btn-inverse")"""
    }

    def topicFilter(buttonID: String, topic: Topic) = 
    {
        this.currentProject = None
        this.currentTopic = Some(topic)

        updateList() &
        JqSetHtml("current", topic.title.is) &
        """$('#showAll').prop("disabled", false)""" &
        """$('#current').attr("class", "btn btn-info")"""
    }

    def projectFilter(buttonID: String, project: Project) =
    {
        this.currentProject = Some(project)
        this.currentTopic = None

        updateList() &
        JqSetHtml("current", project.title.is) &
        """$('#showAll').prop("disabled", false)""" &
        """$('#current').attr("class", "btn btn-success")"""
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
            ".action [id]"    #> ("row" + action.idField) &
            ".collapse [id]"  #> ("desc" + action.stuff.idField) &
            ".title *"        #> stuff.title &
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
        val hasCurrentTopic = 
            currentTopic.map(scheduled.action.topics.contains).getOrElse(true)

        val hasCurrentProject = 
            currentProject.map(scheduled.action.projects.contains).getOrElse(true)
   
        hasCurrentTopic && hasCurrentProject && !scheduled.action.stuff.isTrash.is
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
            case "thisWeekTab"  => "本週"
            case "thisMonthTab" => "本月"
            case "allTab" => "全部"
        }

        val intervalAction = tabID match {
            case "thisWeekTab"  => weekAction
            case "thisMonthTab" => monthAction
            case "allTab" => allAction
        }

        updateList(tabID, title, intervalAction)
    }

    def updateList(tabID: String, title: String, intervalAction: List[Scheduled]): JsCmd = 
    {
        val (todayList, intervalList, doneList, outdatedList) = createActionList(intervalAction)

        """$('.intervalTab').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(tabID) &
        JqSetHtml("intervalLabel", title)  &
        JqSetHtml("intervalList", intervalList.flatMap(createActionRow)) &
        JqSetHtml("todayList", todayList.flatMap(createActionRow)) &
        JqSetHtml("doneList", doneList.flatMap(createActionRow)) &
        JqSetHtml("outdatedList", outdatedList.flatMap(createActionRow))
    }

    def editPostAction(stuff: Stuff): JsCmd = {
        updateList()
    }

    def showEditForm(scheduled: Scheduled) = 
    {
        val editStuff = new EditScheduledForm(scheduled, editPostAction)

        """$('#stuffEdit').remove()""" &
        AppendHtml("editForm", editStuff.toForm) &
        """prepareStuffEditForm()"""
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
            
            """$('#row%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            new FadeOut("row" + stuff.idField, 0, 500)
        }

        def markDoneFlag(action: Action, isDone: Boolean): JsCmd = 
        {
            val rowID = "row" + action.idField.is
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

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(scheduled)) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#desc" + stuff.idField) &
        ".isDone" #> SHtml.ajaxCheckbox(action.isDone.is, markDoneFlag(action, _))
    }

    def render = 
    {
        val (todayList, intervalList, doneList, outdatedList) = createActionList(weekAction)

        "#showAll [onclick]" #> SHtml.onEvent(s => showAllStuff()) &
        "#thisWeekTab [onclick]" #> SHtml.onEvent(s => updateList("thisWeekTab")) &
        "#thisMonthTab [onclick]" #> SHtml.onEvent(s => updateList("thisMonthTab")) &
        "#allTab [onclick]" #> SHtml.onEvent(s => updateList("allTab")) &
        "#todayList *"    #> todayList.flatMap(createActionRow) &
        "#intervalList *" #> intervalList.flatMap(createActionRow) &
        "#doneList *"     #> doneList.flatMap(createActionRow) &
        "#outdatedList *" #> outdatedList.flatMap(createActionRow)
    }
}
