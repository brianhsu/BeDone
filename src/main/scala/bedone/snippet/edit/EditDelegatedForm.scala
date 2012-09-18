package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftmodules.combobox._

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

    val projectCombobox = {

        val options = 
            ("placeholder" -> """<i class="icon-folder-open"> </i> 請選擇專案""") :: Nil

        new ComboBox(None, true, options) {

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

            override def onSearching(term: String): List[ComboItem] = {
                Project.findByUser(currentUser).openOr(Nil)
                       .filter(p => p.title.is.contains(term))
                       .map(p => ComboItem(p.idField.toString, p.title.is))
            }
    
            override def onItemSelected(item: Option[ComboItem]): JsCmd = {
                item match {
                    case None => Noop
                    case Some(selected) =>
                        val project = Project.findByID(selected.id.toInt).get
                        addProject(project)
                }
            }
    
            override def onItemAdded(name: String): JsCmd = {
                val userID = CurrentUser.get.get.idField.is
                val topic = Project.createRecord.userID(userID).title(name)
                addProject(topic)
            }
        }
    }

    val topicCombobox = {

        val options = ("placeholder" -> """<i class="icon-tag"> </i> 請選擇主題""") :: Nil

        new ComboBox(None, true, options) {

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

            override def onSearching(term: String): List[ComboItem] = {
                Topic.findByUser(currentUser).openOr(Nil)
                       .filter(t => t.title.is.contains(term))
                       .map(t => ComboItem(t.idField.toString, t.title.is))
            }
    
            override def onItemSelected(item: Option[ComboItem]): JsCmd = {
                item match {
                    case None => Noop
                    case Some(selected) =>
                        val topic = Topic.findByID(selected.id.toInt).get
                        addTopic(topic)
                }
            }
    
            override def onItemAdded(name: String): JsCmd = {
                val userID = CurrentUser.get.get.idField.is
                val topic = Topic.createRecord.userID(userID).title(name)
                addTopic(topic)
            }
        }
    }

    val contactCombobox = {

        def onSearching(term: String): List[ComboItem] = {
            Contact.findByUser(currentUser).openOr(Nil)
                   .filter(c => c.name.is.contains(term))
                   .map(c => ComboItem(c.idField.toString, c.name.is))
        }

        def onItemSelected(item: Option[ComboItem]): JsCmd = {
            for (selected <- item) {
                currentContact = Contact.findByID(selected.id.toInt).toOption
            }
        }

        def onItemAdded(name: String): JsCmd = {
            val newContact = Contact.createRecord.name(name).userID(currentUser.idField.is)
            currentContact = Some(newContact)
        }

        val defaultItem = currentContact.map(c => ComboItem(c.idField.toString, c.name.is))

        ComboBox(defaultItem, onSearching _, onItemSelected _, onItemAdded _)
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
        ".delegateProjectCombo" #> projectCombobox.comboBox &
        ".delegateTopicCombo" #> topicCombobox.comboBox &
        ".delegateContactCombo" #> contactCombobox.comboBox &
        "#delegateCancel [onclick]" #> SHtml.onEvent(x => FadeOutAndRemove("delegateEdit")) &
        "#delegateSave [onclick]" #> SHtml.onEvent(x => save()) &
        "#delegateSave *" #> (if (stuff.isPersisted) "儲存" else "新增")
    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

