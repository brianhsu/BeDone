package org.bedone.lib

import net.liftweb.common._
import net.liftweb.util.Helpers._

import java.util.Calendar
import java.text.SimpleDateFormat

object CalendarUtils
{
    private implicit def optFromStr(x: String) = Option(x).filterNot(_.trim.length == 0)

    private lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    def getCalendarDate(dateString: String): Box[Calendar] = 
    {
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

    def getCalendar(dateTimeString: String): Option[Calendar] = 
    {
        dateTimeFormatter.setLenient(false)

        optFromStr(dateTimeString) match {
            case None => None
            case Some(dateTimeString) =>
                try {
                    dateTimeFormatter.setLenient(false)
                    val dateTime = dateTimeFormatter.parse(dateTimeString)
                    val calendar = Calendar.getInstance
                    calendar.setTime(dateTime)
                    Some(calendar)
                } catch {
                    case e => None
                }
        }
    }

}

