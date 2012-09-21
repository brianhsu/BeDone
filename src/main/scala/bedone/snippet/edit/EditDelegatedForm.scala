package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._
import org.bedone.lib.TagButton.Implicit._

import net.liftmodules.combobox._

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.jquery.JqJsCmds._


class EditDelegatedForm(user: User, delegated: Delegated, 
                        postAction: Stuff => JsCmd) extends JSImplicit
{
    private implicit def optFromStr(x: String) = Option(x).filterNot(_.trim.length == 0)

    private def template = Templates("templates-hidden" :: "delegated" :: "edit" :: Nil)

    private lazy val currentUser = CurrentUser.get.get
    private lazy val action = delegated.action
    private lazy val stuff = action.stuff

    private var topic: Option[String] = _
    private var project: Option[String] = _

    private var currentTopics: List[Topic] = stuff.topics
    private var currentProjects: List[Project] = stuff.projects
    private var currentContact: Option[Contact] = Option(delegated.contact)

    val projectCombobox = new ProjectComboBox {
        def addProject(project: Project) = {
            currentProjects.map(_.title.is).contains(project.title.is) match {
                case true  => this.clear
                case false =>
                    currentProjects ::= project
                    this.clear &
                    AppendHtml(
                        "delegateProjectTags", 
                        project.editButton(onProjectClick, onProjectRemove)
                    )
            }
        }
    }

    val topicCombobox = new TopicComboBox{
        def addTopic(topic: Topic) = {
            currentTopics.map(_.title.is)contains(topic.title.is) match {
                case true  => this.clear
                case false =>
                    currentTopics ::= topic
                    this.clear &
                    AppendHtml(
                        "delegateTopicTags", 
                        topic.editButton(onTopicClick, onTopicRemove)
                    )
            }
        }
    }

    val contactCombobox = new ContactComboBox {
        override def setContact(contact: Option[Contact]) = {
            currentContact = contact
        }
    }

    def doNothing(s: String) {}
    def onTopicClick(buttonID: String, topic: Topic) = Noop
    def onProjectClick(buttonID: String, project: Project) = Noop

    def onProjectRemove(buttonID: String, project: Project) = 
    {
        currentProjects = currentProjects.filterNot(_ == project)
        FadeOutAndRemove(buttonID)
    }

    def onTopicRemove(buttonID: String, topic: Topic) = 
    {
        currentTopics = currentTopics.filterNot(_ == topic)
        FadeOutAndRemove(buttonID)
    }

    def setTitle(title: String): JsCmd = 
    {
        val errors = stuff.title(title).title.validate
        setError(errors, "delegateTitle")._2
    }

    def setDescription(desc: String): JsCmd = 
    {
        stuff.description(desc)
        Noop
    }

    def save(): JsCmd = 
    {
        val status = List(
            setError(stuff.title.validate, "delegateTitle")
        )

        val hasError = status.map(_._1).contains(true) || currentContact.isEmpty
        val jsCmds = "$('#delegateSave').button('reset')" & status.map(_._2)

        hasError match {
            case true  => jsCmds
            case false => 
                stuff.saveTheRecord()
                stuff.setTopics(currentTopics)
                stuff.setProjects(currentProjects)

                currentContact.foreach { contact => 
                    contact.saveTheRecord()
                    delegated.contactID(contact.idField.is).saveTheRecord()
                }

                FadeOutAndRemove("delegateEdit") & postAction(stuff)
        }
    }

    def cssBinder = 
    {
        val titleInput = SHtml.textAjaxTest(stuff.title.is, doNothing _, setTitle _)
        val contactName = delegated.contact.name.is
        val projectTags = currentProjects.map(_.editButton(onProjectClick, onProjectRemove))
        val topicTags = currentTopics.map(_.editButton(onTopicClick, onTopicRemove))

        "#delegateTitle" #> ("input" #> titleInput) &
        "#delegateEditDesc" #> SHtml.ajaxTextarea(stuff.description.is, setDescription _) &
        "#delegateTopicTags *" #> topicTags &
        "#delegateProjectTags *" #> projectTags &
        "#delegateProjectCombo" #> projectCombobox.comboBox &
        "#delegateTopicCombo" #> topicCombobox.comboBox &
        "#delegateContactCombo" #> contactCombobox.comboBox &
        "#delegateCancel [onclick]" #> SHtml.onEvent(x => FadeOutAndRemove("delegateEdit")) &
        "#delegateSave [onclick]" #> SHtml.onEvent(x => save()) &
        "#delegateSave *" #> (if (stuff.isPersisted) "儲存" else "新增")
    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

