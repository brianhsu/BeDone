package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common.Box
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

class EditScheduledForm(scheduled: Scheduled, postAction: Stuff => JsCmd) extends JSImplicit
{
    private implicit def optFromStr(x: String) = Option(x).filterNot(_.trim.length == 0)

    private def template = Templates("templates-hidden" :: "scheduled" :: "edit" :: Nil)

    private lazy val action = scheduled.action
    private lazy val stuff = action.stuff

    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
    private lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")

    private var topic: Option[String] = _
    private var project: Option[String] = _

    private var currentTopics: List[Topic] = stuff.topics
    private var currentProjects: List[Project] = stuff.projects
 
    def addTopic(title: String) = 
    {
        val userID = CurrentUser.get.get.idField.is
        def createTopic = Topic.createRecord.userID(userID).title(title)
        val topic = Topic.findByTitle(userID, title).getOrElse(createTopic)

        currentTopics.contains(topic) match {
            case true => ClearValue("scheduledTopic")
            case false =>
                currentTopics ::= topic
                ClearValue("scheduledTopic") &
                AppendHtml("scheduledTopicTags", topic.editButton(onTopicClick, onTopicRemove))
        }
    }

    def addProject(title: String) = 
    {
        val userID = CurrentUser.get.get.idField.is
        def createProject = Project.createRecord.userID(userID).title(title)
        val project = Project.findByTitle(userID, title).getOrElse(createProject)

        currentProjects.contains(project) match {
            case true  => ClearValue("scheduledProject")
            case false =>
                currentProjects ::= project
                ClearValue("scheduledProject") &
                AppendHtml(
                    "scheduledProjectTags", 
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
        val errors = stuff.title(title).title.validate
        setError(errors, "scheduledTitle")._2
    }

    def dateTimeFromStr(dateTimeStr: String, defaultValue: Box[Calendar]) = 
    {
        optFromStr(dateTimeStr) match {
            case None    => defaultValue
            case Some(x) => try {
                dateTimeFormatter.setLenient(false)
                val calendar = Calendar.getInstance
                calendar.setTime(dateTimeFormatter.parse(x))
                Full(calendar)
            } catch {
                case e: ParseException => Failure("日期格式錯誤")
            }
        }

    }

    def setEndTime(endTimeStr: String): JsCmd = 
    {
        val endTime = dateTimeFromStr(endTimeStr, Empty)
        scheduled.endTime.setBox(endTime)
        val errors = scheduled.endTime.validate
        setError(errors, "scheduledEndTime")._2
    }

    def setStartTime(startTimeStr: String): JsCmd = 
    {
        val startTime = dateTimeFromStr(startTimeStr, Failure("此為必填欄位"))

        scheduled.startTime.setBox(startTime)

        val errors = scheduled.startTime.validate
        setError(errors, "scheduledStartTime")._2
    }

    def setLocation(location: String): JsCmd = 
    {
        scheduled.location(location)
        Noop
    }

    def setDescription(desc: String): JsCmd = 
    {
        stuff.description(desc)
        Noop
    }

    def save(): JsCmd = {

        val status = List(
            setError(stuff.title.validate, "scheduledTitle"),
            setError(scheduled.startTime.validate, "scheduledStartTime"),
            setError(scheduled.endTime.validate, "scheduledEndTime")
        )

        val hasError = status.map(_._1).contains(true)
        val jsCmds = "$('#scheduledSave').button('reset')" & status.map(_._2)

        hasError match {
            case true  => jsCmds
            case false => 
                stuff.saveTheRecord()
                stuff.setTopics(currentTopics)
                stuff.setProjects(currentProjects)
                scheduled.saveTheRecord()
                FadeOutAndRemove("scheduledEdit") & postAction(stuff)
        }
    }

    def cssBinder = {

        val startTime = dateTimeFormatter.format(scheduled.startTime.is.getTime)
        val endTime = scheduled.endTime.is.map(x => dateTimeFormatter.format(x.getTime))
                               .getOrElse("")

        val titleInput = SHtml.textAjaxTest(stuff.title.is, doNothing _, setTitle _)
        val startTimeInput = SHtml.textAjaxTest(startTime, doNothing _, setStartTime _)
        val endTimeInput = SHtml.textAjaxTest(endTime, doNothing _, setEndTime _)
        val locationInput = SHtml.textAjaxTest(
            scheduled.location.is.getOrElse(""), doNothing _,
            setLocation _
        )

        val topicTags = currentTopics.map(_.editButton(onTopicClick, onTopicRemove))
        val projectTags = currentProjects.map(_.editButton(onProjectClick, onProjectRemove))

        "#scheduledTitle" #> ("input" #> titleInput) &
        "#scheduledEditDesc" #> SHtml.ajaxTextarea(stuff.description.is, setDescription _) &
        "#scheduledTopic" #> (SHtml.text("", topic = _)) &
        "#scheduledTopicHidden" #> (SHtml.hidden(addTopic)) &
        "#scheduledProject" #> (SHtml.text("", project = _)) &
        "#scheduledProjectHidden" #> (SHtml.hidden(addProject)) &
        "#scheduledTopicTags *" #> topicTags &
        "#scheduledProjectTags *" #> projectTags &
        "#scheduledStartTime" #> ("input" #> startTimeInput) &
        "#scheduledEndTime" #> ("input" #> endTimeInput) &
        "#scheduleLocation" #> ("input" #> locationInput) &
        "#scheduledCancel [onclick]" #> SHtml.onEvent(x => FadeOutAndRemove("scheduledEdit")) &
        "#scheduledSave [onclick]" #> SHtml.onEvent(x => save()) &
        "#scheduledSave *" #> (if (stuff.isPersisted) "儲存" else "新增")
    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

