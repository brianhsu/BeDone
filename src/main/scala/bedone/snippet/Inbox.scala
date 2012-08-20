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
    private var rapidTitle: String = _

    private lazy val currentUser = CurrentUser.get.get
    private lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private def stuffs = Stuff.findByUser(currentUser).openOr(Nil).filterNot(_.isTrash.is)
    private def completeStuffTable = createStuffTable(stuffs)

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
            
            """$('#inboxRow%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            new FadeOut("inboxRow" + stuff.idField, 0, 500)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(stuff)) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#desc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility
    }

    def topicFilter(buttonID: String, topic: Topic): JsCmd = 
    {
        val newTable = createStuffTable(topic.stuffs)

        JqSetHtml("inboxList", newTable) &
        JqSetHtml("inboxCurrent", topic.title.is) &
        JsRaw("""$('#inboxShowAll').prop("disabled", false)""") &
        JsRaw("""$('#inboxCurrent').attr("class", "btn btn-info")""")
    }

    def projectFilter(buttonID: String, project: Project): JsCmd = 
    {
        val newTable = createStuffTable(project.stuffs)

        JqSetHtml("inboxList", newTable) &
        JqSetHtml("inboxCurrent", project.title.is) &
        JsRaw("""$('#inboxShowAll').prop("disabled", false)""") &
        JsRaw("""$('#inboxCurrent').attr("class", "btn btn-success")""")
    }

    def showAllStuff() = 
    {
        JqSetHtml("inboxList", completeStuffTable) & 
        JqSetHtml("inboxCurrent", "全部") &
        JsRaw("""$('#inboxShowAll').prop("disabled", true)""") &
        JsRaw("""$('#inboxCurrent').attr("class", "btn btn-inverse")""")
    }

    def createStuffTable(stuffs: List[Stuff]) = stuffs.map(createStuffRow).flatten

    def createStuffRow(stuff: Stuff) = {

        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "stuff" :: "item" :: Nil)

        val cssBinding = 
            actionBar(stuff) &
            ".stuffs [id]"   #> ("inboxRow" + stuff.idField) &
            ".collapse [id]" #> ("desc" + stuff.idField) &
            ".title *"       #> stuff.title &
            ".desc *"        #> stuff.descriptionHTML &
            ".topic *"       #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"     #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".deadline"      #> formatDeadline(stuff)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def showInsertForm(): JsCmd = 
    {
        def userID = CurrentUser.is.map(_.idField.is).get
        def createNewStuff: Stuff = Stuff.createRecord.userID(userID)

        val editStuff = new EditStuffForm(createNewStuff, {stuff =>
            AppendHtml("inboxList", createStuffRow(stuff))
        })

        """$('#stuffEdit').remove()""" &
        AppendHtml("inboxEditHolder", editStuff.toForm) &
        Run("prepareInboxEditForm()")
    }

    def showEditForm(stuff: Stuff): JsCmd = 
    {
        val editStuff = new EditStuffForm(stuff, editPostAction _)

        """$('#stuffEdit').remove()""" &
        AppendHtml("inboxEditHolder", editStuff.toForm) &
        Run("prepareInboxEditForm()")
    }

    def editPostAction(stuff: Stuff): JsCmd = 
    {
        val newRow = createStuffRow(stuff).flatMap(_.child)
        JqSetHtml("inboxRow" + stuff.idField.is, newRow)
    }

    def addRapidStuff(): JsCmd =
    {
        val stuff = Stuff.createRecord.userID(currentUser.idField.is).title(rapidTitle)

        stuff.saveTheRecord()

        """$('#inboxRapidStuff').val("")""" &
        AppendHtml("inboxList", createStuffRow(stuff))
    }

    def render = 
    {
        "#inboxShowAll" #> SHtml.ajaxButton("顯示全部", showAllStuff _) &
        "#inboxAdd [onclick]" #> SHtml.onEvent(s => showInsertForm) &
        "#inboxList *" #> completeStuffTable &
        "#inboxRapidStuff" #> SHtml.text("", rapidTitle = _) &
        "#inboxRapidTitle" #> SHtml.hidden(addRapidStuff _)
    }
}
