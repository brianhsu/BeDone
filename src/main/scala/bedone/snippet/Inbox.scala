package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

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

class Inbox extends JSImplicit
{

    lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    def stuffs = CurrentUser.get.flatMap(Stuff.findByUser).openOr(Nil)
    def completeStuffTable = createStuffTable(stuffs)

    def formatDeadline(stuff: Stuff) = 
    {
        stuff.deadline.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateFormatter.format(calendar.getTime)
        }
    }

    def filter(topic: Topic)(): JsCmd =
    {
        val newTable = createStuffTable(topic.stuffs)

        JqSetHtml("stuffTable", newTable) &
        JqSetHtml("current", Text(topic.title.is)) &
        JsRaw("""$('#showAll').prop("disabled", false)""") &
        JsRaw("""$('#current').attr("class", "btn btn-info")""")

    }
    
    def filter(project: Project)(): JsCmd =
    {
        val newTable = createStuffTable(project.stuffs)

        JqSetHtml("stuffTable", newTable) &
        JqSetHtml("current", Text(project.title.is)) &
        JsRaw("""$('#showAll').prop("disabled", false)""") &
        JsRaw("""$('#current').attr("class", "btn btn-success")""")
    }

    def showAllStuff() = 
    {
        JqSetHtml("stuffTable", completeStuffTable) & 
        JqSetHtml("current", Text("全部")) &
        JsRaw("""$('#showAll').prop("disabled", true)""")
    }

    def actionBar(stuff: Stuff) = {

        def starClass = stuff.isStared.is match {
            case true  => "myicon-starOn"
            case false => "myicon-starOff"
        }

        def toogleStar(): JsCmd = {
            stuff.isStared(!stuff.isStared.is)
            stuff.update()
            
            """$('#row%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.update()

            new FadeOut("row" + stuff.idField, 0, 500)
        }

        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#desc" + stuff.idField)
    }

    def createStuffTable(stuffs: List[Stuff]) = 
    {
        val template = Templates("templates-hidden" :: "stuffTable" :: Nil)
        val cssBinding = 
            ".stuffs" #> stuffs.filter(!_.isTrash.is).map ( stuff =>
                actionBar(stuff) &
                ".stuffs [id]"  #> ("row" + stuff.idField) &
                ".collapse [id]" #> ("desc" + stuff.idField) &
                ".title *" #> stuff.title &
                ".desc *"  #> stuff.descriptionHTML &
                ".topic"   #> stuff.topics.map{ topic =>
                    "a [onclick]" #> SHtml.onEvent(s => filter(topic)) &
                    ".title" #> topic.title.is
                } &
                ".project" #> stuff.projects.map{ project =>
                    "a [onclick]" #> SHtml.onEvent(s => filter(project)) &
                    ".title" #> project.title.is
                } &
                ".deadline"   #> formatDeadline(stuff)
            )

        template.map(cssBinding).open_!
    }

    def stuffTable = 
    {
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
