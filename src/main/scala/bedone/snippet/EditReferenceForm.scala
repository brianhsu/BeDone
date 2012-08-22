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

class EditReferenceForm(stuff: Stuff, postAction: Stuff => JsCmd) extends JSImplicit
{
    private implicit def optFromStr(x: String) = Option(x).filterNot(_.trim.length == 0)

    private def template = Templates("templates-hidden" :: "reference" :: "edit" :: Nil)

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
            case true => ClearValue("referenceTopic")
            case false =>
                currentTopics ::= topic
                ClearValue("referenceTopic") &
                AppendHtml("referenceTopicTags", topic.editButton(onTopicClick, onTopicRemove))
        }
    }

    def addProject(title: String) = {
        val userID = CurrentUser.get.get.idField.is
        def createProject = Project.createRecord.userID(userID).title(title)
        val project = Project.findByTitle(userID, title).getOrElse(createProject)

        currentProjects.contains(project) match {
            case true  => ClearValue("referenceProject")
            case false =>
                currentProjects ::= project
                ClearValue("referenceProject") &
                AppendHtml(
                    "referenceProjectTags", 
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
        setError(stuff.title.validate, "referenceTitle")._2
    }

    def setDescription(desc: String): JsCmd = {
        stuff.description(desc)
        Noop
    }

    def save(): JsCmd = {

        val status = List(
            setError(stuff.title.validate, "referenceTitle")
        )

        val hasError = status.map(_._1).contains(true)
        val jsCmds = "$('#referenceSave').button('reset')" & status.map(_._2)

        hasError match {
            case true  => jsCmds
            case false => 
                stuff.saveTheRecord()
                stuff.setTopics(currentTopics)
                stuff.setProjects(currentProjects)
                FadeOutAndRemove("referenceEdit") & postAction(stuff)
        }
    }

    def cssBinder = {

        val titleInput = SHtml.textAjaxTest(stuff.title.is, doNothing _, setTitle _)
        val projectTags = currentProjects.map(_.editButton(onProjectClick, onProjectRemove))
        val topicTags = currentTopics.map(_.editButton(onTopicClick, onTopicRemove))

        "#referenceTitle" #> ("input" #> titleInput) &
        "#referenceEditDesc" #> SHtml.ajaxTextarea(stuff.description.is, setDescription _) &
        "#referenceTopic" #> (SHtml.text("", topic = _)) &
        "#referenceTopicHidden" #> (SHtml.hidden(addTopic)) &
        "#referenceProject" #> (SHtml.text("", project = _)) &
        "#referenceProjectHidden" #> (SHtml.hidden(addProject)) &
        "#referenceTopicTags *" #>  topicTags &
        "#referenceProjectTags *" #> projectTags &
        "#referenceCancel [onclick]" #> SHtml.onEvent(x => FadeOutAndRemove("referenceEdit")) &
        "#referenceSave [onclick]" #> SHtml.onEvent(x => save()) &
        "#referenceSave *" #> (if (stuff.isPersisted) "儲存" else "新增")
    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

