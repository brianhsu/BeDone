package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common.Full
import net.liftweb.common.Empty
import net.liftweb.common.Failure

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.jquery.JqJsCmds._


import java.text.SimpleDateFormat
import java.text.ParseException
import java.util.Calendar

import TagButton.Implicit._

class EditActionForm(action: Action, postAction: Stuff => JsCmd) extends JSImplicit
{
    private implicit def optFromStr(x: String) = Option(x).filterNot(_.trim.length == 0)

    private def template = Templates("templates-hidden" :: "action" :: "edit" :: Nil)

    private lazy val stuff = action.stuff
    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var topic: Option[String] = _
    private var project: Option[String] = _
    private var context: Option[String] = _

    private var currentTopics: List[Topic] = stuff.topics
    private var currentProjects: List[Project] = stuff.projects
    private var currentContexts: List[Context] = action.contexts
 
    def addContext(title: String) = 
    {
        val userID = CurrentUser.get.get.idField.is
        def createContext = Context.createRecord.userID(userID).title(title)
        val context = Context.findByTitle(userID, title).getOrElse(createContext)

        currentContexts.contains(context) match {
            case true => ClearValue("actionContext")
            case false =>
                currentContexts ::= context
                ClearValue("actionContext") &
                AppendHtml("actionContextTags", context.editButton(onContextClick, onContextRemove))
        }
    }

    def addTopic(title: String) = 
    {
        val userID = CurrentUser.get.get.idField.is
        def createTopic = Topic.createRecord.userID(userID).title(title)
        val topic = Topic.findByTitle(userID, title).getOrElse(createTopic)

        currentTopics.contains(topic) match {
            case true => ClearValue("actionTopic")
            case false =>
                currentTopics ::= topic
                ClearValue("actionTopic") &
                AppendHtml("actionTopicTags", topic.editButton(onTopicClick, onTopicRemove))
        }
    }

    def addProject(title: String) = 
    {
        val userID = CurrentUser.get.get.idField.is
        def createProject = Project.createRecord.userID(userID).title(title)
        val project = Project.findByTitle(userID, title).getOrElse(createProject)

        currentProjects.contains(project) match {
            case true  => ClearValue("actionProject")
            case false =>
                currentProjects ::= project
                ClearValue("actionProject") &
                AppendHtml(
                    "actionProjectTags", 
                    project.editButton(onProjectClick, onProjectRemove)
                )
        }
    }

    def addContext(): JsCmd = context match {
        case None => Noop
        case Some(context) => addContext(context)
    }

    def addTopic(): JsCmd = topic match {
        case None => Noop
        case Some(title) => addTopic(title)
    }

    def addProject(): JsCmd = project match {
        case None => Noop
        case Some(title) => addProject(title)
    }

    def doNothing(s: String) {}
    def onTopicClick(buttonID: String, topic: Topic) = Noop
    def onProjectClick(buttonID: String, project: Project) = Noop
    def onContextClick(buttonID: String, project: Context) = Noop

    def onContextRemove(buttonID: String, context: Context) = {
        (currentContexts.size - 1)  match {
            case 0 => Alert("至少需要一個 Context")
            case _ =>
                currentContexts = currentContexts.filterNot(_ == context)
                FadeOutAndRemove(buttonID)
        }
    }

    def onProjectRemove(buttonID: String, project: Project) = {
        currentProjects = currentProjects.filterNot(_ == project)
        FadeOutAndRemove(buttonID)
    }

    def onTopicRemove(buttonID: String, topic: Topic) = {
        currentTopics = currentTopics.filterNot(_ == topic)
        FadeOutAndRemove(buttonID)
    }

    def setTitle(title: String): JsCmd = {
        val errors = stuff.title(title).title.validate
        setError(errors, "actionTitle")._2
    }

    def setDeadline(deadline: String): JsCmd = {
        val newDeadline = optFromStr(deadline) match {
            case None    => Empty
            case Some(x) => try {
                dateFormatter.setLenient(false)
                val calendar = Calendar.getInstance
                calendar.setTime(dateFormatter.parse(x))
                Full(calendar)
            } catch {
                case e: ParseException => Failure("日期格式錯誤")
            }
        }

        stuff.deadline.setBox(newDeadline)
        val errors = stuff.deadline.validate
        setError(errors, "actionDeadline")._2
    }

    def setDescription(desc: String): JsCmd = {
        stuff.description(desc)
        Noop
    }

    def save(): JsCmd = {

        val status = List(
            setError(stuff.title.validate, "actionTitle"),
            setError(stuff.deadline.validate, "actionDeadline")
        )

        val hasError = status.map(_._1).contains(true)
        val jsCmds = "$('#actionSave').button('reset')" & status.map(_._2)

        hasError match {
            case true  => jsCmds
            case false => 
                stuff.saveTheRecord()
                stuff.setTopics(currentTopics)
                stuff.setProjects(currentProjects)
                action.setContexts(currentContexts)
                FadeOutAndRemove("actionEdit") & postAction(stuff)
        }
    }

    def cssBinder = {

        val deadline = stuff.deadline.is.map(x => dateFormatter.format(x.getTime)).getOrElse("")

        val titleInput = SHtml.textAjaxTest(stuff.title.is, doNothing _, setTitle _)
        val deadlineInput = SHtml.textAjaxTest(deadline, doNothing _, setDeadline _)

        val contextTags = currentContexts.map(_.editButton(onContextClick, onContextRemove))
        val projectTags = currentProjects.map(_.editButton(onProjectClick, onProjectRemove))

        "#actionTitle" #> ("input" #> titleInput) &
        "#actionEditDesc" #> SHtml.ajaxTextarea(stuff.description.is, setDescription _) &
        "#actionDeadline" #> ("input" #> deadlineInput) &
        "#actionContext" #> (SHtml.text("", context = _)) &
        "#actionContextHidden" #> (SHtml.hidden(addContext)) &
        "#actionTopic" #> (SHtml.text("", topic = _)) &
        "#actionTopicHidden" #> (SHtml.hidden(addTopic)) &
        "#actionProject" #> (SHtml.text("", project = _)) &
        "#actionProjectHidden" #> (SHtml.hidden(addProject)) &
        "#actionContextTags *" #> contextTags &
        "#actionTopicTags *" #> currentTopics.map(_.editButton(onTopicClick, onTopicRemove)) &
        "#actionProjectTags *" #> projectTags &
        "#actionCancel [onclick]" #> SHtml.onEvent(x => FadeOutAndRemove("actionEdit")) &
        "#actionSave [onclick]" #> SHtml.onEvent(x => save()) &
        "#actionSave *" #> (if (stuff.isPersisted) "儲存" else "新增")
    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

