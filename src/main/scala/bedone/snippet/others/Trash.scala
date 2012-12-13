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

    private def stuffs = Trash.findByUser(currentUser).openOr(Nil)
    private def trashs = new Paging(stuffs.filter(shouldDisplay).toArray, 10, 10, onSwitchPage)

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentPage: Int = 1


    def onSwitchPage(paging: Paging[Stuff], page: Int) = {
        currentPage = page
        updateList
    }

    def formatDeadline(stuff: Stuff) = 
    {
        stuff.deadline.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateFormatter.format(calendar.getTime)
        }
    }

    def topicFilter(buttonID: String, topic: Topic): JsCmd = 
    {
        currentTopic = Some(topic)
        currentProject = None

        updateList &
        JqSetHtml("trashCurrent", topic.title.is) &
        JsRaw("""$('#trashShowAll').prop("disabled", false)""") &
        JsRaw("""$('#trashCurrent').attr("class", "btn btn-info")""")
    }

    def projectFilter(buttonID: String, project: Project): JsCmd = 
    {
        currentTopic = None
        currentProject = Some(project)

        updateList &
        JqSetHtml("trashCurrent", project.title.is) &
        JsRaw("""$('#trashShowAll').prop("disabled", false)""") &
        JsRaw("""$('#trashCurrent').attr("class", "btn btn-success")""")
    }

    def showAllStuff() = 
    {
        currentTopic = None
        currentProject = None

        updateList &
        JqSetHtml("trashCurrent", "全部") &
        JsRaw("""$('#trashShowAll').prop("disabled", true)""") &
        JsRaw("""$('#trashCurrent').attr("class", "btn btn-inverse")""")
    }

    def shouldDisplay(stuff: Stuff) = {
        currentTopic.map(p => stuff.hasTopic(p.idField.is)).getOrElse(true) &&
        currentProject.map(t => stuff.hasProject(t.idField.is)).getOrElse(true)
    }

    def updateList: JsCmd = {
        val paged = trashs

        if (currentPage > paged.totalPage) {
            currentPage = 1
        }

        JqSetHtml("trashList", paged(currentPage).flatMap(createTrashRow)) &
        JqSetHtml("trashPageSelector", paged.pageSelector(currentPage))
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
            stuff.isTrash(false)
            stuff.saveTheRecord()

            FadeOutWithCallback("trashRow" + stuff.idField) {
                updateList
            }
        }

        def delete(): JsCmd = {
            Stuff.delete(stuff)

            FadeOutWithCallback("trashRow" + stuff.idField) {
                updateList
            }
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

        trashs.data.foreach(Stuff.delete)

        "$('.trashRow').fadeOut(500)" &
        updateList
    }
   
    def render = {
        val paged = trashs
        "#trashShowAll" #> SHtml.ajaxButton("顯示全部", showAllStuff _) &
        "#emptyTrash [onclick]" #> SHtml.onEvent(s => Confirm("確定清空垃圾桶嗎？", SHtml.ajaxInvoke(emptyTrash))) &
        ".trashRow" #> paged(currentPage).flatMap(createTrashRow) &
        "#trashPageSelector *" #> paged.pageSelector(currentPage)
    }
}
