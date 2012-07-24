package org.bedone.snippet

import org.bedone.model._

import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml.ElemAttr
import scala.xml.NodeSeq
import java.text.SimpleDateFormat

class Inbox
{
    lazy val stuffs = CurrentUser.get.flatMap(Stuff.findByUser _).openOr(Nil)

    val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    def formatDeadline(stuff: Stuff) = {
        stuff.deadline.is.map(c => dateFormatter.format(c.getTime)).getOrElse("")
    }

    def render = {

        ".stuffs" #> stuffs.map ( stuff =>
            ".title *" #> stuff.title &
            ".desc *"  #> stuff.description &
            ".createTime *" #> dateTimeFormatter.format(stuff.createTime.is.getTime) &
            ".deadline *" #> formatDeadline(stuff)
        )
    }
}
