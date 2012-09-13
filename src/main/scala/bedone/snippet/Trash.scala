package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common.Box

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import java.text.SimpleDateFormat
import scala.xml.NodeSeq

class Trash extends  JSImplicit
{

    private val currentUser = CurrentUser.get.get
    private def trashs = Trash.findByUser(currentUser).openOr(Nil)

    def formatDeadline(stuff: Stuff) = 
    {
        stuff.deadline.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateFormatter.format(calendar.getTime)
        }
    }

    def topicFilter(buttonID: String, topic: Topic): JsCmd = 
    {
        val trashs = this.trashs.filter(_.topics.exists(_.idField.is == topic.idField.is))
        val newTable = trashs.flatMap(createTrashRow)

        JqSetHtml("trashList", newTable) &
        JqSetHtml("trashCurrent", topic.title.is) &
        JsRaw("""$('#trashShowAll').prop("disabled", false)""") &
        JsRaw("""$('#trashCurrent').attr("class", "btn btn-info")""")
    }

    def projectFilter(buttonID: String, project: Project): JsCmd = 
    {
        val trashs = this.trashs.filter(_.projects.exists(_.idField.is == project.idField.is))
        val newTable = trashs.flatMap(createTrashRow)

        JqSetHtml("trashList", newTable) &
        JqSetHtml("trashCurrent", project.title.is) &
        JsRaw("""$('#trashShowAll').prop("disabled", false)""") &
        JsRaw("""$('#trashCurrent').attr("class", "btn btn-success")""")
    }

    def showAllStuff() = 
    {
        JqSetHtml("trashList", trashs.flatMap(createTrashRow)) & 
        JqSetHtml("trashCurrent", "全部") &
        JsRaw("""$('#trashShowAll').prop("disabled", true)""") &
        JsRaw("""$('#trashCurrent').attr("class", "btn btn-inverse")""")
    }

    def actionBar(stuff: Stuff) = {

        def starClass = stuff.isStared.is match {
            case true  => "myicon-starOn"
            case false => "myicon-starOff"
        }

        def toogleStar(): JsCmd = {
            stuff.isStared(!stuff.isStared.is)
            stuff.saveTheRecord()
            
            """$('#trashRow%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def undelete(): JsCmd = {
            println("undelete " + stuff.idField.is)
            stuff.isTrash(false)
            stuff.saveTheRecord()
            new FadeOut("trashRow" + stuff.idField, 0, 500)
        }

        def delete(): JsCmd = {
            println("delete " + stuff.idField.is)
            Stuff.delete(stuff)
            new FadeOut("trashRow" + stuff.idField, 0, 500)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".undelete [onclick]" #> SHtml.onEvent(s => undelete) &
        ".remove [onclick]" #> SHtml.onEvent(s => delete) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#trashDesc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility
    }

    def createTrashRow(stuff: Stuff) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "trash" :: "item" :: Nil)

        val cssBinding = 
            actionBar(stuff) &
            ".trash [id]"    #> ("trashRow" + stuff.idField) &
            ".collapse [id]" #> ("trashDesc" + stuff.idField) &
            ".title *"       #> stuff.titleWithLink &
            ".desc *"        #> stuff.descriptionHTML &
            ".deadline"      #> formatDeadline(stuff) &
            ".topic *"       #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"     #> stuff.projects.map(_.viewButton(projectFilter)).flatten

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def emptyTrash(): JsCmd = {
        
        trashs.foreach(Stuff.delete)

        "$('.trashRow').fadeOut(500)"
    }
   
    def render = {
        "#trashShowAll" #> SHtml.ajaxButton("顯示全部", showAllStuff _) &
        "#emptyTrash [onclick]" #> SHtml.onEvent(s => Confirm("確定清空垃圾桶嗎？", SHtml.ajaxInvoke(emptyTrash))) &
        ".trashRow" #> trashs.flatMap(createTrashRow)
    }
}
