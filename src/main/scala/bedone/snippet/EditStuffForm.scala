package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw

import scala.xml.NodeSeq
import scala.xml.Text
import net.liftweb.record.Record
import java.text.SimpleDateFormat

class EditStuffForm(stuff: Stuff)(postAction: => JsCmd) extends JSImplicit
{
    lazy val template = Templates("templates-hidden" :: "editStuff" :: Nil)
    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var topic: Option[String] = _
    
    def setTopic(s: String) { topic = if (s.length > 0) Some(s) else None }

    def addTopic(): JsCmd = {
        println("AddTopic")
        
        """$('.inputTopic').val('')"""
    }

    def cssBinder = {
        val deadline = stuff.deadline.is.map(x => dateFormatter.format(x.getTime)).getOrElse("")

        ".title [value]" #> stuff.title &
        ".inputTopic" #> (SHtml.text("", setTopic _)) &
        ".inputTopicHidden" #> (SHtml.hidden(addTopic)) &
        ".desc *" #> stuff.description &
        ".deadline [value]" #> deadline &
        "#QQQQ" #> "QQQQ"
    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

