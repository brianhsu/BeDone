package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib.CalendarUtils._

import net.liftweb.common._
import net.liftweb.http.js.JsCmd
import net.liftweb.util.FieldError

import java.util.Calendar

trait TicklerPicker extends HasStuff
{
    var tickler: Box[Calendar] = Empty

    def setTickler(dateString: String, 
                   onOK: => JsCmd, onError: List[FieldError] => JsCmd): JsCmd = 
    {
        this.tickler = getCalendarDate(dateString)
        val errors = stuff.map(x => Maybe.createRecord).toList.flatMap { maybe => 
            maybe.tickler.setBox(tickler)
            maybe.tickler.validate
        }

        errors match {
            case Nil => onOK
            case xs  => onError(xs)
        }
    }
}

