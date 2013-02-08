package org.bedone.lib

import net.liftweb.common._
import net.liftweb.util.Helpers._

import java.util.Calendar
import java.text.SimpleDateFormat

object CalendarUtils
{
    private implicit def optFromStr(x: String) = Option(x).filterNot(_.trim.length == 0)

    val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    def getCalendarDate(dateString: String): Box[Calendar] = {
         optFromStr(dateString) match {
            case None => Empty
            case Some(date) => tryo {
                dateFormatter.setLenient(false)
                val date = dateFormatter.parse(dateString)
                val calendar = Calendar.getInstance
                calendar.setTime(date)
                calendar
            }
         }
    }

}

