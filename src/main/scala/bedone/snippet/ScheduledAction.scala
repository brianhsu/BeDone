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
    lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    val currentUser = CurrentUser.get.get
    def scheduledAction = Scheduled.findByUser(currentUser).openOr(Nil)
    def todayAction = scheduledAction.filter(isToday)
    def weekAction  = scheduledAction.filter(isThisWeek)
    def monthAction = scheduledAction.filter(isThisMonth)


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
        import net.liftweb.util.TimeHelpers._

        val todayStart = (new DateMidnight())
        val todayEnd = (new DateMidnight()).plusDays(1)

        scheduled.startTime.is.getTime.after(todayStart.toDate) &&
        scheduled.startTime.is.getTime.before(todayEnd.toDate)
    }

    def render = 
    {
        println("today:" + todayAction.map(_.action.stuff.title))
        println("thisWeek:" + weekAction.map(_.action.stuff.title))
        println("thisMonth:" + monthAction.map(_.action.stuff.title))
        println("all:" + scheduledAction.map(_.action.stuff.title))
        "aaa" #> "QQQQ"
    }
}
