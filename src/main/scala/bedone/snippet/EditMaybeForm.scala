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

class EditMaybeForm(maybe: Maybe, postAction: Stuff => JsCmd) extends JSImplicit
{
    private implicit def optFromStr(x: String) = Option(x).filterNot(_.trim.length == 0)

    private def template = Templates("templates-hidden" :: "maybe" :: "edit" :: Nil)

    private lazy val stuff = maybe.stuff

    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

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
            case true => ClearValue("maybeTopic")
            case false =>
                currentTopics ::= topic
                ClearValue("maybeTopic") &
                AppendHtml("maybeTopicTags", topic.editButton(onTopicClick, onTopicRemove))
        }
    }

    def addProject(title: String) = 
    {
        val userID = CurrentUser.get.get.idField.is
        def createProject = Project.createRecord.userID(userID).title(title)
        val project = Project.findByTitle(userID, title).getOrElse(createProject)

        currentProjects.contains(project) match {
            case true  => ClearValue("maybeProject")
            case false =>
                currentProjects ::= project
                ClearValue("maybeProject") &
                AppendHtml(
                    "maybeProjectTags", 
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
        setError(errors, "maybeTitle")._2
    }

    def dateFromStr(dateStr: String, defaultValue: Box[Calendar]) = 
    {
        optFromStr(dateStr) match {
            case None    => defaultValue
            case Some(x) => try {
                dateFormatter.setLenient(false)
                val calendar = Calendar.getInstance
                calendar.setTime(dateFormatter.parse(x))
                Full(calendar)
            } catch {
                case e: ParseException => Failure("日期格式錯誤")
            }
        }

    }

    def setTickler(dateStr: String): JsCmd = 
    {
        val ticklerDate = dateFromStr(dateStr, Empty)

        maybe.tickler.setBox(ticklerDate)

        val errors = maybe.tickler.validate
        setError(errors, "maybeTicklerDate")._2
    }

    def setDescription(desc: String): JsCmd = 
    {
        stuff.description(desc)
        Noop
    }

    def save(): JsCmd = {

        val status = List(
            setError(stuff.title.validate, "maybeTitle"),
            setError(maybe.tickler.validate, "maybeTicklerDate")
        )

        val hasError = status.map(_._1).contains(true)
        val jsCmds = "$('#maybeSave').button('reset')" & status.map(_._2)

        hasError match {
            case true  => jsCmds
            case false => 
                stuff.saveTheRecord()
                stuff.setTopics(currentTopics)
                stuff.setProjects(currentProjects)
                maybe.saveTheRecord()
                FadeOutAndRemove("maybeEdit") & postAction(stuff)
        }
    }

    def cssBinder = {

        val tickler = maybe.tickler.is
                           .map(calendar => dateFormatter.format(calendar.getTime))
                           .getOrElse("")

        val titleInput = SHtml.textAjaxTest(stuff.title.is, doNothing _, setTitle _)
        val ticklerInput = SHtml.textAjaxTest(tickler, doNothing _, setTickler _)

        val topicTags = currentTopics.map(_.editButton(onTopicClick, onTopicRemove))
        val projectTags = currentProjects.map(_.editButton(onProjectClick, onProjectRemove))

        "#maybeTitle" #> ("input" #> titleInput) &
        "#maybeEditDesc" #> SHtml.ajaxTextarea(stuff.description.is, setDescription _) &
        "#maybeTopic" #> (SHtml.text("", topic = _)) &
        "#maybeTopicHidden" #> (SHtml.hidden(addTopic)) &
        "#maybeProject" #> (SHtml.text("", project = _)) &
        "#maybeProjectHidden" #> (SHtml.hidden(addProject)) &
        "#maybeTopicTags *" #> topicTags &
        "#maybeProjectTags *" #> projectTags &
        "#maybeTicklerDate" #> ("input" #> ticklerInput) &
        "#maybeCancel [onclick]" #> SHtml.onEvent(x => FadeOutAndRemove("maybeEdit")) &
        "#maybeSave [onclick]" #> SHtml.onEvent(x => save()) &
        "#maybeSave *" #> (if (stuff.isPersisted) "儲存" else "新增")
    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

