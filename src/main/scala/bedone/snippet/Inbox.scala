package org.bedone.snippet

import org.bedone.model._

import net.liftweb.util.ClearClearable
import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml.ElemAttr
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import scala.xml.NodeSeq
import scala.xml.Text

import java.text.SimpleDateFormat

class Inbox
{
    lazy val stuffs = CurrentUser.get.flatMap(Stuff.findByUser).openOr(Nil)
    lazy val completeStuffTable = createStuffTable(stuffs)
    lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    def formatDeadline(stuff: Stuff) = 
    {
        stuff.deadline.is.map(c => dateFormatter.format(c.getTime)).getOrElse("")
    }

    def noStuff = ".table" #> "" & "#processButton [disabled]" #> "disabled"

    def filterStuff(topic: String)(): JsCmd =
    {
        val stuffsOfTopic = stuffs.filter(_.topics.openOr(Nil).contains(topic))
        val newTable = createStuffTable(stuffsOfTopic)

        JqSetHtml("stuffTable", newTable) &
        JqSetHtml("current", Text(topic)) &
        JsRaw("""$('#showAll').prop("disabled", false)""")
    }
    
    def showAllStuff() = 
    {
        JqSetHtml("stuffTable", completeStuffTable) & 
        JqSetHtml("current", Text("全部")) &
        JsRaw("""$('#showAll').prop("disabled", true)""")
    }

    def createStuffTable(stuffs: List[Stuff]) = 
    {
        val template = Templates("templates-hidden" :: "stuffTable" :: Nil)
        val cssBinding = 
            ".stuffs" #> stuffs.map ( stuff =>
                ".title *" #> stuff.title &
                ".desc *"  #> stuff.descriptionHTML &
                ".topic" #> stuff.topics.openOr(Nil).map{ topic =>
                    "a" #> SHtml.a(filterStuff(topic)_, Text(topic))
                } &
                ".createTime *" #> dateTimeFormatter.format(stuff.createTime.is.getTime) &
                ".deadline *" #> formatDeadline(stuff)
            )

        template.map(cssBinding).open_!
    }

    def stuffTable = 
    {
        "#noStuff" #> "" &
        "#showAll" #> SHtml.ajaxButton("顯示全部", showAllStuff _) &
        "#stuffTable *" #> completeStuffTable
    }

    def render = 
    {
        stuffs.isEmpty match {
            case true  => noStuff
            case false => stuffTable
        }
    }
}
