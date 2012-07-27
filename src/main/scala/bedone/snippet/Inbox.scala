package org.bedone.snippet

import org.bedone.model._

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

    lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    def stuffs = CurrentUser.get.flatMap(Stuff.findByUser).openOr(Nil)
    def completeStuffTable = createStuffTable(stuffs)

    def formatDeadline(stuff: Stuff) = 
    {
        stuff.deadline.is.map(c => dateFormatter.format(c.getTime)).getOrElse("")
    }

    def filter(topic: Topic)(): JsCmd =
    {
        val newTable = createStuffTable(topic.stuffs)

        JqSetHtml("stuffTable", newTable) &
        JqSetHtml("current", Text(topic.title.is)) &
        JsRaw("""$('#showAll').prop("disabled", false)""")
    }
    
    def filter(project: Project)(): JsCmd =
    {
        val newTable = createStuffTable(project.stuffs)

        JqSetHtml("stuffTable", newTable) &
        JqSetHtml("current", Text(project.title.is)) &
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
                ".topic" #> stuff.topics.map{ topic =>
                    "a" #> SHtml.a(filter(topic)_, Text(topic.title.is))
                } &
                ".project" #> stuff.projects.map{ project =>
                    "a" #> SHtml.a(filter(project)_, Text(project.title.is))
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

    def render = stuffTable

    def addStuffDialog = {

        object InboxAddStuffDialog extends AddStuffDialog {
            override def saveAndClose() = {
                val originJS = super.saveAndClose()
                val newTable = createStuffTable(stuffs)

                originJS & reInitForm & showAllStuff
            }
        }

        InboxAddStuffDialog.render
    }
}
