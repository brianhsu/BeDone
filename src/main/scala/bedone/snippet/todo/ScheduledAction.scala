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

class ScheduledAction extends JSImplicit with ScheduledPredicate
{
    private lazy val currentUser = CurrentUser.get.get
    private lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentTabID: String = "scheduledWeekTab"

    private var currentOutdatedPage: Int = 1
    private var currentTodayPage: Int = 1
    private var currentIntervalPage: Int = 1
    private var currentDonePage: Int = 1

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

    def createActionRow(scheduledT: ScheduledT) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "scheduled" :: "item" :: Nil)

        val scheduled = scheduledT.scheduled
        val action = scheduledT.action
        val stuff = scheduledT.stuff

        val cssBinding = 
            actionBar(scheduled) &
            ".scheduled [id]"    #> ("scheduled" + action.idField) &
            ".collapse [id]"  #> ("scheduledDesc" + action.stuff.idField) &
            ".title *"        #> stuff.titleWithLink &
            ".desc *"         #> stuff.descriptionHTML &
            ".topic *"        #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"      #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".startTime"      #> formatStartTime(scheduled) &
            ".doneTime"       #> formatDoneTime(action) &
            "rel=tooltip [title]" #> createTooltip(scheduled)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def shouldDisplay(scheduledT: ScheduledT) = 
    {
        val stuff = scheduledT.stuff

        currentTopic.map(t => stuff.hasTopic(t.idField.is)).getOrElse(true) &&
        currentProject.map(p => stuff.hasProject(p.idField.is)).getOrElse(true)
    }

    def onSwitchOutdated(paging: Paging[ScheduledT], page: Int) = {
        currentOutdatedPage = page
        currentTodayPage = 1
        currentIntervalPage = 1
        currentDonePage = 1
        updateList()
    }

    def onSwitchToday(paging: Paging[ScheduledT], page: Int) = {
        currentOutdatedPage = 1
        currentTodayPage = page
        currentIntervalPage = 1
        currentDonePage = 1
        updateList()
    }

    def onSwitchInterval(paging: Paging[ScheduledT], page: Int) = {
        currentOutdatedPage = 1
        currentTodayPage = 1
        currentIntervalPage = page
        currentDonePage = 1
        updateList()
    }

    def onSwitchDone(paging: Paging[ScheduledT], page: Int) = {
        currentOutdatedPage = 1
        currentTodayPage = 1
        currentIntervalPage = 1
        currentDonePage = page
        updateList()
    }

    def createActionList(intervalAction: List[ScheduledT]) = 
    {
        val outdatedList = allAction.filter(isOutdated).filter(shouldDisplay)
        val todayList = todayAction.filter(shouldDisplay).filterNot(isOutdated)
        val intervalList = intervalAction.filter(shouldDisplay).filterNot(isOutdated)
        val doneList = 
            todayList.filter(_.action.isDone.is) ++ 
            intervalList.filter(_.action.isDone.is)

        (new Paging(todayList.filterNot(_.action.isDone.is).toArray, 10, 5, onSwitchToday _),
         new Paging(intervalList.filterNot(_.action.isDone.is).toArray, 10, 5, onSwitchInterval _),
         new Paging(doneList.toArray, 10, 5, onSwitchDone _),
         new Paging(outdatedList.toArray, 10, 5, onSwitchOutdated _))
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

    def updateList(tabID: String, title: String, intervalAction: List[ScheduledT]): JsCmd = 
    {
        val (todayList, intervalList, doneList, outdatedList) = createActionList(intervalAction)

        """$('.intervalTab').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(tabID) &
        JqSetHtml("scheduledIntervalLabel", title)  &
        JqSetHtml("scheduledIntervalList", intervalList(currentIntervalPage).flatMap(createActionRow)) &
        JqSetHtml("scheduledTodayList", todayList(currentTodayPage).flatMap(createActionRow)) &
        JqSetHtml("scheduledDoneList", doneList(currentDonePage).flatMap(createActionRow)) &
        JqSetHtml("scheduledOutdatedList", outdatedList(currentOutdatedPage).flatMap(createActionRow)) &
        JqSetVisible("scheduledOutdatedBlock", !outdatedList(currentOutdatedPage).isEmpty) &
        JqSetVisible("scheduledIntervalBlock", !intervalList(currentIntervalPage).isEmpty) &
        JqSetVisible("scheduledTodayBlock", !todayList(currentTodayPage).isEmpty) &
        JqSetVisible("scheduledDoneBlock", !doneList(currentDonePage).isEmpty) &
        JqSetHtml("outdatedPageSelector", outdatedList.pageSelector(currentOutdatedPage)) &
        JqSetHtml("intervalPageSelector", intervalList.pageSelector(currentIntervalPage)) &
        JqSetHtml("todayPageSelector", todayList.pageSelector(currentTodayPage)) &
        JqSetHtml("donePageSelector", doneList.pageSelector(currentDonePage))

    }

    def editPostAction(stuff: Stuff): JsCmd = {
        updateList() &
        """updateNotes()"""
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

            FadeOutWithCallback("scheduled" + stuff.idField) {
                updateList(currentTabID)
            }
        }

        def reInbox(): JsCmd = 
        {
            stuff.reInbox()

            FadeOutWithCallback("scheduled" + stuff.idField.is) {
                updateList(currentTabID)
            }
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
        import scala.xml.NodeSeq

        val (todayList, intervalList, doneList, outdatedList) = createActionList(weekAction)
        val hidden = "display: none;";

        val todayListHTML: NodeSeq = todayList(currentTodayPage).flatMap(createActionRow)
        val intervalListHTML: NodeSeq = intervalList(currentIntervalPage).flatMap(createActionRow)
        val doneListHTML: NodeSeq = doneList(currentDonePage).flatMap(createActionRow)
        val outdatedListHTML: NodeSeq = outdatedList(currentOutdatedPage).flatMap(createActionRow)

        "#scheduledShowAll [onclick]" #> SHtml.onEvent(s => showAllStuff()) &
        "#scheduledWeekTab [onclick]" #> SHtml.onEvent(s => updateList("scheduledWeekTab")) &
        "#scheduledMonthTab [onclick]" #> SHtml.onEvent(s => updateList("scheduledMonthTab")) &
        "#scheduledAllTab [onclick]" #> SHtml.onEvent(s => updateList("scheduledAllTab")) &
        "#scheduledTodayList *"    #> todayListHTML &
        "#scheduledIntervalList *" #> intervalListHTML &
        "#scheduledDoneList *" #> doneListHTML &
        "#scheduledOutdatedList *" #> outdatedListHTML &
        "#scheduledOutdatedBlock [style+]" #> (if (outdatedList(currentOutdatedPage).isEmpty) hidden else "") &
        "#scheduledIntervalBlock [style+]" #> (if (intervalList(currentIntervalPage).isEmpty) hidden else "") &
        "#scheduledTodayBlock [style+]" #> (if (todayList(currentTodayPage).isEmpty) hidden else "") &
        "#scheduledDoneBlock [style+]" #> (if (doneList(currentDonePage).isEmpty) hidden else "") &
        "#outdatedPageSelector *" #> outdatedList.pageSelector(currentOutdatedPage) &
        "#intervalPageSelector *" #> intervalList.pageSelector(currentIntervalPage) &
        "#todayPageSelector *"    #> todayList.pageSelector(currentTodayPage) &
        "#donePageSelector *"     #> doneList.pageSelector(currentDonePage)

    }
}
