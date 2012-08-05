package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

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

    def actionBar(stuff: Stuff) = {

        def starClass = stuff.isStared.is match {
            case true  => "myicon-starOn"
            case false => "myicon-starOff"
        }

        def toogleStar(): JsCmd = {
            stuff.isStared(!stuff.isStared.is)
            stuff.saveTheRecord()
            
            """$('#row%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            new FadeOut("row" + stuff.idField, 0, 500)
        }

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(stuff)) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#desc" + stuff.idField)
    }

    def topicFilter(buttonID: String, topic: Topic): JsCmd = 
    {
        val newTable = createStuffTable(topic.stuffs)

        JqSetHtml("stuffTable", newTable) &
        JqSetHtml("current", topic.title.is) &
        JsRaw("""$('#showAll').prop("disabled", false)""") &
        JsRaw("""$('#current').attr("class", "btn btn-info")""")
    }

    def projectFilter(buttonID: String, project: Project): JsCmd = 
    {
        val newTable = createStuffTable(project.stuffs)

        JqSetHtml("stuffTable", newTable) &
        JqSetHtml("current", project.title.is) &
        JsRaw("""$('#showAll').prop("disabled", false)""") &
        JsRaw("""$('#current').attr("class", "btn btn-success")""")
    }

    def showAllStuff() = 
    {
        JqSetHtml("stuffTable", completeStuffTable) & 
        JqSetHtml("current", "全部") &
        JsRaw("""$('#showAll').prop("disabled", true)""") &
        JsRaw("""$('#current').attr("class", "btn btn-inverse")""")
    }

    def createStuffTable(stuffs: List[Stuff]) = stuffs.map(createStuffRow).flatten

    def createStuffRow(stuff: Stuff) = {

        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "stuff" :: "item" :: Nil)

        val cssBinding = 
            actionBar(stuff) &
            ".stuffs [id]"   #> ("row" + stuff.idField) &
            ".collapse [id]" #> ("desc" + stuff.idField) &
            ".title *"       #> stuff.title &
            ".desc *"        #> stuff.descriptionHTML &
            ".topic *"       #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"     #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".deadline"      #> formatDeadline(stuff)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def editPostAction(stuff: Stuff): JsCmd = {
        val newRow = createStuffRow(stuff).flatMap(_.child)
        JqSetHtml("row" + stuff.idField.is, newRow)
    }

    def showInsertForm(): JsCmd = 
    {
        def userID = CurrentUser.is.map(_.idField.is).get
        def createNewStuff: Stuff = Stuff.createRecord.userID(userID)

        val editStuff = new EditStuffForm(createNewStuff, {stuff =>
            AppendHtml("stuffTable", createStuffRow(stuff))
        })

        """$('#stuffEdit').remove()""" &
        AppendHtml("editForm", editStuff.toForm) &
        """prepareStuffEditForm()"""
    }

    def showEditForm(stuff: Stuff): JsCmd = 
    {
        val editStuff = new EditStuffForm(stuff, editPostAction _)

        """$('#stuffEdit').remove()""" &
        AppendHtml("editForm", editStuff.toForm) &
        """prepareStuffEditForm()"""
    }

    def stuffTable = 
    {
        "#showAll" #> SHtml.ajaxButton("顯示全部", showAllStuff _) &
        "#addStuffButton [onclick]" #> SHtml.onEvent(s => showInsertForm) &
        "#stuffTable *" #> completeStuffTable
    }

    def render = stuffTable
}
