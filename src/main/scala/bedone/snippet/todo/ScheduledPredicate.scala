package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.http.S
import net.liftweb.util.Helpers._

import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat

import org.joda.time._

trait ScheduledPredicate
{
    private lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    def isOutdated(scheduledT: ScheduledT): Boolean = 
    {
        val todayStart = (new DateMidnight())

        scheduledT.scheduled.startTime.is.getTime.before(todayStart.toDate) &&
        !scheduledT.scheduled.action.isDone.is
    }

    def isThisMonth(scheduledT: ScheduledT): Boolean =
    {
        val monthStart = (new DateMidnight).withDayOfMonth(1)
        val monthEnd = monthStart.plusMonths(1)

        scheduledT.scheduled.startTime.is.getTime.after(monthStart.toDate) &&
        scheduledT.scheduled.startTime.is.getTime.before(monthEnd.toDate)
    }

    def isThisWeek(scheduledT: ScheduledT): Boolean = 
    {
        val weekStart = (new DateMidnight).withDayOfWeek(1)
        val weekEnd = weekStart.plusDays(7)

        scheduledT.scheduled.startTime.is.getTime.after(weekStart.toDate) &&
        scheduledT.scheduled.startTime.is.getTime.before(weekEnd.toDate)
    }

    def isToday(scheduledT: ScheduledT): Boolean = 
    {
        val todayStart = (new DateMidnight())
        val todayEnd = (new DateMidnight()).plusDays(1)

        scheduledT.scheduled.startTime.is.getTime.after(todayStart.toDate) &&
        scheduledT.scheduled.startTime.is.getTime.before(todayEnd.toDate)
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

    def createTooltip(scheduled: Scheduled) = 
    {
        val endTime = scheduled.endTime.is.map { x => 
            S.?("End Time:") + "<br>" + dateTimeFormatter.format(x.getTime) + "<br>"
        }
        val location = scheduled.location.is.map(x => S.?("Location:") + "<br>" + x)

        endTime.getOrElse("") + location.getOrElse("")
    }

}

