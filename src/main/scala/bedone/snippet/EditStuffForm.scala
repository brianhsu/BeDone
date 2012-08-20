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

class EditStuffForm(stuff: Stuff, postAction: Stuff => JsCmd) extends JSImplicit
{
    private implicit def optFromStr(x: String) = Option(x).filterNot(_.trim.length == 0)

    private def template = Templates("templates-hidden" :: "stuff" :: "edit" :: Nil)

    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var topic: Option[String] = _
    private var project: Option[String] = _

    private var currentTopics: List[Topic] = stuff.topics
    private var currentProjects: List[Project] = stuff.projects
  
    def addTopic(title: String) = {
        val userID = CurrentUser.get.get.idField.is
        def createTopic = Topic.createRecord.userID(userID).title(title)
        val topic = Topic.findByTitle(userID, title).getOrElse(createTopic)

        currentTopics.contains(topic) match {
            case true => ClearValue("inboxTopic")
            case false =>
                currentTopics ::= topic
                ClearValue("inboxTopic") &
                AppendHtml("inboxTopicTags", topic.editButton(onTopicClick, onTopicRemove))
        }
    }

    def addProject(title: String) = {
        val userID = CurrentUser.get.get.idField.is
        def createProject = Project.createRecord.userID(userID).title(title)
        val project = Project.findByTitle(userID, title).getOrElse(createProject)

        currentProjects.contains(project) match {
            case true  => ClearValue("inboxProject")
            case false =>
                currentProjects ::= project
                ClearValue("inboxProject") &
                AppendHtml(
                    "inboxProjectTags", 
                    project.editButton(onProjectClick, onProjectRemove)
                )
        }
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

    def onProjectRemove(buttonID: String, project: Project) = {
        currentProjects = currentProjects.filterNot(_ == project)
        FadeOutAndRemove(buttonID)
    }

    def onTopicRemove(buttonID: String, topic: Topic) = {
        currentTopics = currentTopics.filterNot(_ == topic)
        FadeOutAndRemove(buttonID)
    }

    def setTitle(title: String): JsCmd = {
        stuff.title(title)
        setError(stuff.title.validate, "inboxTitle")._2
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
        setError(errors, "inboxDeadline")._2
    }

    def setDescription(desc: String): JsCmd = {
        stuff.description(desc)
        Noop
    }

    def save(): JsCmd = {

        val status = List(
            setError(stuff.title.validate, "inboxTitle"),
            setError(stuff.deadline.validate, "inboxDeadline")
        )

        val hasError = status.map(_._1).contains(true)
        val jsCmds = "$('#inboxSave').button('reset')" & status.map(_._2)

        hasError match {
            case true  => jsCmds
            case false => 
                stuff.saveTheRecord()
                stuff.setTopics(currentTopics)
                stuff.setProjects(currentProjects)
                FadeOutAndRemove("inboxEdit") & postAction(stuff)
        }
    }

    def cssBinder = {

        val deadline = stuff.deadline.is.map(x => dateFormatter.format(x.getTime)).getOrElse("")

        val titleInput = SHtml.textAjaxTest(stuff.title.is, doNothing _, setTitle _)
        val deadlineInput = SHtml.textAjaxTest(deadline, doNothing _, setDeadline _)
        val projectTags = currentProjects.map(_.editButton(onProjectClick, onProjectRemove))
        val topicTags = currentTopics.map(_.editButton(onTopicClick, onTopicRemove))

        "#inboxTitle" #> ("input" #> titleInput) &
        "#inboxDesc" #> SHtml.ajaxTextarea(stuff.description.is, setDescription _) &
        "#inboxDeadline" #> ("input" #> deadlineInput) &
        "#inboxTopic" #> (SHtml.text("", topic = _)) &
        "#inboxTopicHidden" #> (SHtml.hidden(addTopic)) &
        "#inboxProject" #> (SHtml.text("", project = _)) &
        "#inboxProjectHidden" #> (SHtml.hidden(addProject)) &
        "#inboxTopicTags *" #>  topicTags &
        "#inboxProjectTags *" #> projectTags &
        "#inboxCancel [onclick]" #> SHtml.onEvent(x => FadeOutAndRemove("inboxEdit")) &
        "#inboxSave [onclick]" #> SHtml.onEvent(x => save()) &
        "#inboxSave *" #> (if (stuff.isPersisted) "儲存" else "新增")
    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

