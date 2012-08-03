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

import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat

class NextAction extends JSImplicit
{
    lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    val currentUser = CurrentUser.get.get
    def contexts = Context.findByUser(currentUser).openOr(Nil)

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentContext: Option[Context] = contexts.headOption

    def formatDoneTime(action: Action) = 
    {
        action.doneTime.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateTimeFormatter.format(calendar.getTime)
        }
    }

    def formatDeadline(action: Action) = 
    {
        val isDone = action.isDone.is

        def dateBinding(calendar: Calendar) = {
            ".label *" #> dateFormatter.format(calendar.getTime)
        }

        action.stuff.deadline.is match {
            case Some(calendar) if !isDone => dateBinding(calendar)
            case _ => "*" #> ""
        }
    }

    def actionBar(action: Action) = 
    {
        val stuff = action.stuff

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

        def markDoneFlag(action: Action, isDone: Boolean): JsCmd = 
        {
            val rowID = "row" + action.idField.is
            val doneTime = isDone match {
                case false => None
                case true  =>
                    val calendar = Calendar.getInstance
                    calendar.setTime(new Date)
                    Some(calendar)
            }

            action.isDone(isDone)
            action.doneTime(doneTime)
            action.saveTheRecord()

            updateList &
            Hide(rowID) &
            new FadeIn(rowID, 200, 2500)
        }

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(action)) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#desc" + stuff.idField) &
        ".isDone" #> SHtml.ajaxCheckbox(action.isDone.is, markDoneFlag(action, _))
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

    def editPostAction(stuff: Stuff): JsCmd = {
        updateList()
    }

    def showEditForm(action: Action) = 
    {
        val editStuff = new EditActionForm(action, editPostAction)

        """$('#stuffEdit').remove()""" &
        AppendHtml("editForm", editStuff.toForm) &
        """prepareStuffEditForm()"""
    }


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
        def byDoneTime(action1: Action, action2: Action) = {
            val time1: Long = action1.doneTime.is.map(_.getTime.getTime).getOrElse(0)
            val time2: Long = action2.doneTime.is.map(_.getTime.getTime).getOrElse(0)
            time1 < time2
        }

        val (doneList, notDoneList) = actions
        val doneHTML = 
            doneList.filter(shouldDisplay).sortWith(byDoneTime)
                    .map(createActionRow).flatten

        val notDoneHTML = notDoneList.filter(shouldDisplay).map(createActionRow).flatten
        val contextTab = contexts.map(createContextTab)

        JqEmpty("isDone") &
        JqEmpty("notDone") &
        JqSetHtml("isDone", doneHTML) &
        JqSetHtml("notDone", notDoneHTML) &
        JqSetHtml("contextTabFolder", contextTab.flatten)
    }

    def createActionRow(action: Action) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "action" :: "item" :: Nil)

        val stuff = action.stuff

        val cssBinding = 
            actionBar(action) &
            ".action [id]"    #> ("row" + action.idField) &
            ".collapse [id]"  #> ("desc" + action.stuff.idField) &
            ".title *"        #> stuff.title &
            ".desc *"         #> stuff.descriptionHTML &
            ".topic"          #> action.topics.map(_.viewButton(topicFilter)) &
            ".project"        #> action.projects.map(_.viewButton(projectFilter)) &
            ".deadline"       #> formatDeadline(action) &
            ".doneTime"       #> formatDoneTime(action)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def switchContext(context: Context, str: String) = {

        val contextTabID = ("contextTab" + context.idField.is)
        this.currentContext = Some(context)

        """$('.contextTab').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(contextTabID) &
        updateList()
    }

    def createContextTab(context: Context) = 
    {
        val contextTabID = ("contextTab" + context.idField.is)
        val activtedStyle = if (currentContext == Some(context)) "active" else ""
        val cssBinding = 
            "li [class]"  #> activtedStyle &
            "li [id]"     #> contextTabID &
            "a *"         #> context.title &
            "a [onclick]" #> SHtml.onEvent(switchContext(context, _))

        cssBinding(<li class="contextTab"><a href="#">@ Home</a></li>)
    }

    def render = 
    {
        val (doneActions, notDoneActions) = actions

        ClearClearable &
        "#showAll"   #> SHtml.ajaxButton("顯示全部", showAllStuff _) &
        "#isDone *"  #> doneActions.filter(shouldDisplay).map(createActionRow).flatten &
        "#notDone *" #> notDoneActions.filter(shouldDisplay).map(createActionRow).flatten &
        "#contextTabFolder *" #> contexts.map(createContextTab).flatten
    }
}
