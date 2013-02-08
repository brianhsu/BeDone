package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib.CalendarUtils._

import java.util.Calendar
import java.text.SimpleDateFormat

import net.liftweb.common._
import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import net.liftweb.util.FieldError


trait DeadlinePicker extends HasStuff
{
    def setDeadline(dateString: String, onOK: => JsCmd, 
                    onError: List[FieldError] => JsCmd): JsCmd = 
    {

        val deadline = getCalendarDate(dateString)
        val errors = stuff.toList.flatMap { s =>
            s.deadline.setBox(deadline)
            s.deadline.validate
        }

        errors match {
            case Nil => onOK
            case xs  => onError(xs)
        }
    }
}

