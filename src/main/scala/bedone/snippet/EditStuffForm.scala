package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.jquery.JqJsCmds._

import scala.xml.NodeSeq
import scala.xml.Text
import net.liftweb.record.Record
import java.text.SimpleDateFormat

import TagButton.Implicit._


class EditStuffForm(stuff: Stuff)(postAction: => JsCmd) extends JSImplicit
{
    private def template = Templates("templates-hidden" :: "editStuff" :: Nil)

    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    private var topic: Option[String] = _
    private var project: Option[String] = _

    private var currentTopics: List[Topic] = stuff.topics
    private var currentProjects: List[Project] = stuff.projects
  
    def setTopic(s: String) { topic = if (s.length > 0) Some(s) else None }
    def setProject(s: String) { project = if (s.length > 0) Some(s) else None }

    def addTopic(title: String) = {
        val userID = CurrentUser.get.get.idField.is
        def createTopic = Topic.createRecord.userID(userID).title(title)
        val topic = Topic.findByTitle(userID, title).getOrElse(createTopic)

        currentTopics.contains(topic) match {
            case true => ClearValue("inputTopic")
            case false =>
                currentTopics ::= topic
                ClearValue("inputTopic") &
                AppendHtml("editStuffTopics", topic.editButton(onTopicClick, onTopicRemove))
        }
    }

    def addProject(title: String) = {
        val userID = CurrentUser.get.get.idField.is
        def createProject = Project.createRecord.userID(userID).title(title)
        val project = Project.findByTitle(userID, title).getOrElse(createProject)

        currentProjects.contains(project) match {
            case true  => ClearValue("inputProject")
            case false =>
                currentProjects ::= project
                ClearValue("inputProject") &
                AppendHtml(
                    "editStuffProjects", 
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

    def cssBinder = {
        val deadline = stuff.deadline.is.map(x => dateFormatter.format(x.getTime)).getOrElse("")

        ".title [value]" #> stuff.title &
        ".desc *" #> stuff.description &
        ".deadline [value]" #> deadline &
        "#inputTopic" #> (SHtml.text("", setTopic _)) &
        "#inputTopicHidden" #> (SHtml.hidden(addTopic)) &
        "#inputProject" #> (SHtml.text("", setProject _)) &
        "#inputProjectHidden" #> (SHtml.hidden(addProject)) &
        "#editStuffTopics *" #> currentTopics.map(_.editButton(onTopicClick, onTopicRemove)) &
        "#editStuffProjects *" #> currentProjects.map(_.editButton(onProjectClick, onProjectRemove))
    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

