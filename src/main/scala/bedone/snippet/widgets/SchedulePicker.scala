package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib.CalendarUtils._

import net.liftweb.common._

import net.liftweb.http.S
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._

import net.liftweb.util.FieldError
import net.liftweb.util.Helpers._

import java.util.Calendar

trait SchedulePicker extends HasStuff
{
    var startTime: Box[Calendar] = None
    var endTime: Option[Calendar] = None
    var location: Option[String] = None

    def setLocation(location: String): JsCmd = 
    {
        this.location = location
    }

    def validateSchedule: List[FieldError] =
    {
        stuff.map { todo =>
            val scheduled = Scheduled.createRecord
            scheduled.startTime.setBox(startTime)
            scheduled.endTime.set(endTime)
            scheduled.location.set(location)
            scheduled.validate
        }.flatten.toList
    }

    def setEndTime(dateTimeString: String, 
                   onOK: => JsCmd, 
                   onError: List[FieldError] => JsCmd): JsCmd = 
    {
        endTime = getCalendar(dateTimeString)

        val validation = stuff.map { todo =>
            val scheduled = Scheduled.createRecord
            scheduled.startTime.setBox(startTime)
            scheduled.endTime.set(endTime)
            scheduled.location.set(location)
            scheduled.endTime.validate
        }.flatten.toList

        validation match {
            case Nil => onOK
            case errors => onError(errors)
        }
    }


    def setStartTime(dateTimeString: String, 
                     onOK: => JsCmd,
                     onError: List[FieldError] => JsCmd): JsCmd = 
    {
        startTime = getCalendar(dateTimeString)

        val validation = stuff.map { todo =>
            val scheduled = Scheduled.createRecord
            scheduled.startTime.setBox(startTime)
            scheduled.endTime.set(endTime)
            scheduled.location.set(location)
            scheduled.startTime.validate
        }.flatten.toList

        validation match {
            case Nil => onOK
            case errors => onError(errors)
        }
    }

}

