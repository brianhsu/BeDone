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

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentContext: Option[Context] = contexts.headOption
    private var currentAction: Option[Action] = None

    def formatDeadline(stuff: Stuff) = 
    {
        stuff.deadline.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateFormatter.format(calendar.getTime)
        }
    }

    def showAllStuff() = 
    {
        this.currentTopic = None
        this.currentProject = None

        updateList() &
        JqSetHtml("current", "全部") &
        """$('#showAll').prop("disabled", true)""" &
        """$('#current').attr("class", "btn btn-inverse")"""
    }

    def topicFilter(buttonID: String, topic: Topic) = 
    {
        this.currentTopic = Some(topic)

        updateList() &
        JqSetHtml("current", topic.title.is) &
        """$('#showAll').prop("disabled", false)""" &
        """$('#current').attr("class", "btn btn-info")"""
    }

    def projectFilter(buttonID: String, project: Project) =
    {
        this.currentProject = Some(project)

        updateList() &
        JqSetHtml("current", project.title.is) &
        """$('#showAll').prop("disabled", false)""" &
        """$('#current').attr("class", "btn btn-success")"""
    }

    def showEditForm(stuff: Stuff) = Noop


    def shouldDisplay(action: Action) = 
    {
        val hasCurrentTopic = currentTopic.map(action.topics.contains).getOrElse(true)
        val hasCurrentProject = currentProject.map(action.projects.contains).getOrElse(true)
        val hasCurrentContext = currentContext.map(action.contexts.contains).getOrElse(true)
   
        hasCurrentTopic && 
        hasCurrentProject &&
        hasCurrentContext
    }

    def actions: (List[Action], List[Action]) = {
        Action.findByUser(currentUser).openOr(Nil)
              .filterNot(_.stuff.isTrash.is)
              .partition(_.isDone.is)
    }

    def updateList(): JsCmd =
    {
        val (doneList, notDoneList) = actions
        val doneHTML = doneList.filter(shouldDisplay).map(createActionRow).flatten
        val notDoneHTML = notDoneList.filter(shouldDisplay).map(createActionRow).flatten

        this.currentAction = None

        JqEmpty("isDone") &
        JqEmpty("notDone") &
        JqSetHtml("isDone", doneHTML) &
        JqSetHtml("notDone", notDoneHTML)
    }

    def updateList(action: Action)(isDone: Boolean): JsCmd = 
    {
        val rowID = "row" + action.idField.is

        currentAction = Some(action)
        action.isDone(isDone)
        action.saveTheRecord()

        updateList &
        new FadeIn(rowID, 200, 2500)
    }

    def createActionRow(action: Action) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "action" :: "item" :: Nil)

        val stuff = action.stuff
        val displayStyle = currentAction match {
            case Some(current) if current.idField.is == action.idField.is => "display: none"
            case _ => "display: block"
        }

        val cssBinding = 
            ".action [style]" #> displayStyle &
            ".action [id]"   #> ("row" + action.idField) &
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

    def switchContext(context: Context, str: String) = {

        val contextTabID = ("contextTab" + context.idField.is)
        this.currentContext = Some(context)

        """$('.contextTab').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(contextTabID) &
        updateList()
    }

    def render = 
    {
        val (doneActions, notDoneActions) = actions

        ClearClearable &
        "#showAll"   #> SHtml.ajaxButton("顯示全部", showAllStuff _) &
        "#isDone *"  #> doneActions.filter(shouldDisplay).map(createActionRow).flatten &
        "#notDone *" #> notDoneActions.filter(shouldDisplay).map(createActionRow).flatten &
        ".contextTab" #> contexts.map { context =>

            val contextTabID = ("contextTab" + context.idField.is)
            val activtedStyle = if (currentContext == Some(context)) "active" else ""

            "li [class]"  #> activtedStyle &
            "li [id]"     #> contextTabID &
            "a *"         #> context.title &
            "a [onclick]" #> SHtml.onEvent(switchContext(context, _))
        }
    }
}
