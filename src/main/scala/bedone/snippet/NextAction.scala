package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common.Box

import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds._

import net.liftweb.http.S
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
    private var currentContext: Option[Context] = None

    private val topicID: Box[Int] = S.attr("topicID").map(_.toInt)
    private val projectID: Box[Int] = S.attr("projectID").map(_.toInt)

    private def allActions = Action.findByUser(currentUser).openOr(Nil)
    private def projectAction = projectID.map(Action.findByProject(currentUser, _).openOr(Nil))
    private def topicAction = topicID.map(Action.findByTopic(currentUser, _).openOr(Nil))

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
            
            """$('#action%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            FadeOutAndRemove("action" + stuff.idField.is)
        }

        def reInbox(): JsCmd = 
        {
            stuff.reInbox()
            FadeOutAndRemove("action" + stuff.idField.is)
        }

        def markDoneFlag(action: Action, isDone: Boolean): JsCmd = 
        {
            val rowID = "action" + action.idField.is
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

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(action)) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) & 
        ".reinbox [onclick]" #> SHtml.onEvent(s => reInbox) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#actionDesc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility &
        ".isDone" #> SHtml.ajaxCheckbox(action.isDone.is, markDoneFlag(action, _))
    }

    def showAllStuff() = 
    {
        this.currentTopic = None
        this.currentProject = None

        updateList() &
        JqSetHtml("actionCurrent", "全部") &
        """$('#actionShowAll').prop("disabled", true)""" &
        """$('#actionCurrent').attr("class", "btn btn-inverse")"""
    }

    def topicFilter(buttonID: String, topic: Topic) = 
    {
        this.currentProject = None
        this.currentTopic = Some(topic)

        updateList() &
        JqSetHtml("actionCurrent", topic.title.is) &
        """$('#actionShowAll').prop("disabled", false)""" &
        """$('#actionCurrent').attr("class", "btn btn-info")"""
    }

    def projectFilter(buttonID: String, project: Project) =
    {
        this.currentProject = Some(project)
        this.currentTopic = None

        updateList() &
        JqSetHtml("actionCurrent", project.title.is) &
        """$('#actionShowAll').prop("disabled", false)""" &
        """$('#actionCurrent').attr("class", "btn btn-success")"""
    }

    def editPostAction(stuff: Stuff): JsCmd = {
        updateList()
    }

    def showEditForm(action: Action) = 
    {
        val editStuff = new EditActionForm(action, editPostAction)

        """$('#actionEdit').remove()""" &
        SetHtml("actionEditHolder", editStuff.toForm)
    }


    def shouldDisplay(actionT: ActionT) = 
    {
        val stuff = actionT.stuff
        val action = actionT.action

        currentTopic.map(p => stuff.hasTopic(p.idField.is)).getOrElse(true) &&
        currentProject.map(t => stuff.hasProject(t.idField.is)).getOrElse(true) &&
        currentContext.map(action.contexts.contains).getOrElse(true)
    }

    def actions: (List[ActionT], List[ActionT]) = {

        val actions = (projectAction orElse topicAction).getOrElse(allActions)

        val (done, notDone) = actions.partition(_.action.isDone.is)

        (done.filter(shouldDisplay), notDone.filter(shouldDisplay))
    }

    def deleteContext(context: Context)(): JsCmd = {

        this.currentContext = None

        Context.delete(context)

        FadeOutAndRemove("actionTab" + context.idField.is) &
        showAllAction("")
    }

    def updateList(): JsCmd =
    {
        def deleteJS(context: Context) = Confirm(
            "確定要刪除「%s」嗎？" format(context.title.is), 
            SHtml.ajaxInvoke(deleteContext(context))
        )

        val (doneList, notDoneList) = actions
        val doneHTML = doneList.map(createActionRow).flatten
        val notDoneHTML = notDoneList.map(createActionRow).flatten
        val updateDeleteButton = currentContext match {
            case None => Hide("deleteContext")
            case Some(context) => 
                Show("deleteContext") &
                """$('#deleteContext').attr('onclick', '%s')""".format(
                    deleteJS(context).toJsCmd
                )
        }

        JqEmpty("actionIsDone") &
        JqEmpty("actionNotDone") &
        JqSetHtml("actionIsDone", doneHTML) &
        JqSetHtml("actionNotDone", notDoneHTML) &
        updateDeleteButton
    }

    def createActionRow(actionT: ActionT) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "action" :: "item" :: Nil)

        val action = actionT.action
        val stuff = action.stuff

        val cssBinding = 
            actionBar(action) &
            ".action [id]"    #> ("action" + action.idField) &
            ".collapse [id]"  #> ("actionDesc" + action.stuff.idField) &
            ".title *"        #> stuff.titleWithLink &
            ".desc *"         #> stuff.descriptionHTML &
            ".topic *"        #> action.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"      #> action.projects.map(_.viewButton(projectFilter)).flatten &
            ".deadline"       #> formatDeadline(action) &
            ".doneTime"       #> formatDoneTime(action)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def switchContext(context: Context, str: String) = {

        val contextTabID = ("actionTab" + context.idField.is)
        this.currentContext = Some(context)

        """$('.actionTab').removeClass('active')""" &
        """$('#%s').addClass('active')""".format(contextTabID) &
        updateList()
    }

    def createContextTab(context: Context) = 
    {
        val contextTabID = ("actionTab" + context.idField.is)
        val activtedStyle = if (currentContext == Some(context)) "active" else ""

        "li [class]"  #> activtedStyle &
        "li [id]"     #> contextTabID &
        "a *"         #> ("@ " + context.title.is) &
        "a [onclick]" #> SHtml.onEvent(switchContext(context, _))
    }

    def showAllAction(attrValue: String) = {
        this.currentContext = None

        """$('.actionTab').removeClass('active')""" &
        """$('#allActionTab').addClass('active')""" &
        updateList()
    }

    def render = 
    {
        val (doneActions, notDoneActions) = actions

        ClearClearable &
        "#actionShowAll" #> SHtml.ajaxButton("顯示全部", showAllStuff _) &
        "#actionIsDone"  #> (".row" #> doneActions.map(createActionRow)) &
        "#actionNotDone" #> (".row" #> notDoneActions.map(createActionRow)) &
        "#allActionTab [onclick]" #> SHtml.onEvent(showAllAction _) &
        ".contextTab" #> contexts.map(createContextTab)
    }
}
