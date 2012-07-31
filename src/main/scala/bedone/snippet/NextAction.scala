package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds._

import net.liftweb.http.SHtml
import net.liftweb.http.Templates

import net.liftweb.util.ClearClearable
import java.text.SimpleDateFormat

class NextAction extends JSImplicit
{
    lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    val currentUser = CurrentUser.get.get
    val contexts = Context.findByUser(currentUser).openOr(Nil)
    def doneActions = Action.findByUser(currentUser).openOr(Nil).filter(_.isDone.is)
    def notDoneActions = Action.findByUser(currentUser).openOr(Nil).filterNot(_.isDone.is)

    def formatDeadline(stuff: Stuff) = 
    {
        stuff.deadline.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateFormatter.format(calendar.getTime)
        }
    }

    def topicFilter(buttonID: String, topic: Topic) = Noop
    def projectFilter(buttonID: String, project: Project) = Noop
    def showEditForm(stuff: Stuff) = Noop

    def updateList(action: Action)(isDone: Boolean): JsCmd = 
    {
        action.isDone(isDone)
        action.saveTheRecord()

        val doneList = doneActions.map(createActionRow).flatten
        val notDoneList = notDoneActions.map(createActionRow).flatten

        JqSetHtml("isDone", doneList) &
        JqSetHtml("notDone", notDoneList)
    }

    def createActionRow(action: Action) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "action" :: "item" :: Nil)

        val stuff = action.stuff

        val cssBinding = 
            ".stuffs [id]"   #> ("row" + action.idField) &
            ".collapse [id]" #> ("desc" + action.stuff.idField) &
            ".title *"       #> stuff.title &
            ".desc *"        #> stuff.descriptionHTML &
            ".topic"         #> action.topics.map(_.viewButton(topicFilter)) &
            ".project"       #> action.projects.map(_.viewButton(projectFilter)) &
            ".deadline"      #> formatDeadline(stuff) &
            ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(stuff)) &
            ".isDone"         #> SHtml.ajaxCheckbox(action.isDone.is, updateList(action)_)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def render = 
    {
        println(doneActions)
        println(notDoneActions)

        ClearClearable &
        "#notDone *" #> notDoneActions.map(createActionRow) &
        "#isDone *"  #> doneActions.map(createActionRow) &
        ".contextTab" #> contexts.zipWithIndex.map { case (context, i) =>
            "li [class]" #> (if(i == 0) "active" else "") &
            "a *"        #> context.title
        }
    }
}
