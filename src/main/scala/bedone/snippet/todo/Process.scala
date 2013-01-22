package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import org.bedone.model.StuffType.StuffType
import TagButton.Implicit._

import net.liftmodules.combobox._

import net.liftweb.common.Box
import net.liftweb.common.Empty
import net.liftweb.common.Full
import net.liftweb.common.Failure

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml

import net.liftweb.http.js.JE.JsTrue
import net.liftweb.http.js.JE.Str
import net.liftweb.http.js.JsExp
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds._

import java.util.Calendar
import java.text.SimpleDateFormat

class Process extends JSImplicit
{
    private implicit def optFromStr(x: String) = Option(x).filterNot(_.trim.length == 0)

    private lazy val currentUser = CurrentUser.is.get
    private lazy val stuff = Stuff.findByUser(currentUser).openOr(Nil)
                                  .headOption

    private var currentProjects = stuff.map(_.projects).getOrElse(Nil)
    private var currentTopics = stuff.map(_.topics).getOrElse(Nil)
    private var currentContexts: List[Context] = Nil

    // Stuff attirbute
    private var projectTitle: Option[String] = None
    private var topicTitle: Option[String] = None
    private var description: Option[String] = stuff.map(_.description.is)

    // Action attribute
    private var actionTitle: Option[String] = stuff.map(_.title.is)
    private var contextTitle: Option[String] = None

    // Delegated attribute
    private var currentContact: Option[Contact] = None

    // Scheduled attribute
    private var startDateTime: Option[Calendar] = None
    private var endDateTime: Option[Calendar] = None
    private var location: Option[String] = None

    // Maybe attribute
    private var tickler: Box[Calendar] = Empty

    private lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    private lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    val contactCombobox = new ContactComboBox {

        override def setContact(selected: Option[Contact]): JsCmd = {
            selected match {
                case None => """$('#saveDelegated').attr('disabled', true)"""
                case Some(contact) =>
                    currentContact = selected
                    """$('#saveDelegated').attr('disabled', false)"""
            }
        }
    }


    def onTopicClick(buttonID: String, topic: Topic) = Noop
    def onProjectClick(buttonID: String, project: Project) = Noop
    def onContextClick(buttonID: String, context: Context) = Noop

    def onContextRemove(buttonID: String, context: Context): JsCmd = {

        currentContexts = currentContexts.filterNot(_ == context)
        FadeOutAndRemove.byClassName(context.className)
    }

    def onProjectRemove(buttonID: String, project: Project): JsCmd = {

        currentProjects = currentProjects.filterNot(_ == project)
        FadeOutAndRemove.byClassName(project.className)
    }

    def onTopicRemove(buttonID: String, topic: Topic): JsCmd = {

        currentTopics = currentTopics.filterNot(_ == topic)
        FadeOutAndRemove.byClassName(topic.className)
    }

    def saveDelegated(stuff: Stuff)(valueAttr: String): JsCmd = {

        currentContact match {
            case None => Alert(S.?("This field is required."))
            case Some(contact) =>
                stuff.setTopics(currentTopics)
                stuff.setProjects(currentProjects)
                stuff.stuffType(StuffType.Delegated)
                stuff.saveTheRecord()
                contact.saveTheRecord()

                val action = Action.createRecord.idField(stuff.idField.is)
                                   .saveTheRecord().get

                val delegated = Delegated.createRecord.idField(stuff.idField.is)
                                         .contactID(contact.idField.is)
                                         .saveTheRecord()
                
                S.redirectTo(
                    "/todo/process", 
                    () => S.notice(S.?("'%s' is in delegated list now.") format(stuff.title.is))
                )
               
        }
    }

    def saveReference(stuff: Stuff)(valueAttr: String) = {

        updateStuff(stuff, StuffType.Reference)

        S.redirectTo(
            "/todo/process", 
            () => S.notice(S.?("'%s' is in reference list now.") format(stuff.title.is))
        )
    }

    def markAsTrash(stuff: Stuff)(valueAttr: String) = {

        stuff.isTrash(true).saveTheRecord()
        S.redirectTo(
            "/todo/process", 
            () => S.notice(S.?("'%s' is deleted.") format(stuff.title.is))
        )
    }

    def markAsDone(stuff: Stuff)(valueAttr: String) = {

        val updatedStuff = updateStuff(stuff, StuffType.Action)

        Action.createRecord.idField(updatedStuff.idField.is)
              .isDone(true).doneTime(Calendar.getInstance)
              .saveTheRecord()

        S.redirectTo(
            "/todo/process", 
            () => S.notice(S.?("'%s' is marked as done.") format(stuff.title.is))
        )
    }

    def createTopicTags(containerID: String) =
    {
        val topicCombobox = new TopicComboBox{
            val containers = List(
                "editFormTopic", "referenceTopic", 
                "maybeTopic", "nextActionTopic"
            )

            def addTopic(topic: Topic) = {
                currentTopics.map(_.title.is).contains(topic.title.is) match {
                    case true  => this.clear
                    case false =>
                        currentTopics ::= topic
                        this.clear &
                        containers.map { htmlID => 
                            AppendHtml(
                                htmlID, 
                                topic.editButton(onTopicClick, onTopicRemove)
                            )
                        }

                }
            }
        }

        ".topicCombo"     #> topicCombobox.comboBox &
        ".topicTags [id]" #> containerID &
        ".topicTags" #> (
            "span" #> currentTopics.map(_.editButton(onTopicClick, onTopicRemove))
        )
    }

    def createProjectTags(containerID: String) =
    {
        val projectCombobox = new ProjectComboBox {
            val containers = List(
                "editFormProject", "referenceProject", 
                "maybeProject", "nextActionProject"
            )

            def addProject(project: Project) = {
                currentProjects.map(_.title.is).contains(project.title.is) match {
                    case true  => this.clear
                    case false =>
                        currentProjects ::= project
                        this.clear &
                        containers.map { htmlID => 
                            AppendHtml(
                                htmlID, 
                                project.editButton(onProjectClick, onProjectRemove)
                            )
                    }
                }
            }
        }

        ".projectCombo"     #> projectCombobox.comboBox &
        ".projectTags [id]" #> containerID &
        ".projectTags" #> (
            "span" #> currentProjects.map(_.editButton(onProjectClick, onProjectRemove))
        )
    }

    def createContextTags(containerID: String) =
    {
        val contextCombobox = new ContextComboBox{
            val containers = List("nextActionContext")

            def addContext(context: Context) = {
                currentContexts.map(_.title.is).contains(context.title.is) match {
                    case true  => this.clear
                    case false =>
                        currentContexts ::= context
                        this.clear &
                        containers.map { htmlID => 
                            AppendHtml(
                                htmlID, 
                                context.editButton(onContextClick, onContextRemove)
                            )
                        }

                }
            }
        }

        ".contextCombo"     #> contextCombobox.comboBox &
        ".contextTags [id]" #> containerID &
        ".contextTags" #> (
            "span" #> currentContexts.map(_.editButton(onContextClick, onContextRemove))
        )
    }

    def doNothing(s: String) {}

    def setTitle(title: String): JsCmd = {

        val stuffTitle = stuff.map(_.title.is).getOrElse("")

        this.actionTitle = title
        this.actionTitle match {
            case None       => 
                "$('#nextActionTitle').val('%s');".format(stuffTitle) &
                "$('#processTitle input').val('%s');".format(stuffTitle)
            case Some(text) => Noop
                "$('#nextActionTitle').val('%s');".format(text) &
                "$('#processTitle input').val('%s');".format(text)
        }
    }

    def updateStuff(stuff: Stuff, stuffType: StuffType) =
    {
        stuff.title(actionTitle.getOrElse(stuff.title.is))
        stuff.stuffType(stuffType)
        stuff.setProjects(currentProjects)
        stuff.setTopics(currentTopics)
        stuff.description(description.getOrElse(""))
        stuff.saveTheRecord()
        stuff
    }

    def saveNextAction(stuff: Stuff)(valueAttr: String): JsCmd = {
        val updatedStuff = updateStuff(stuff, StuffType.Action)
        val action = Action.createRecord.idField(updatedStuff.idField.is)

        action.saveTheRecord()
        action.setContexts(currentContexts)

        S.redirectTo(
            "/todo/process", 
             () => S.notice(S.?("'%s' is in do it ASAP list now.") format(stuff.title.is))
        )
    }

    def saveMaybe(stuff: Stuff)(valueAttr: String): JsCmd = {

        updateStuff(stuff, StuffType.Maybe)

        val maybe = Maybe.createRecord.idField(stuff.idField.is)

        maybe.tickler(this.tickler.toOption)
        maybe.saveTheRecord()

        S.redirectTo(
            "/todo/process", 
             () => S.notice(S.?("'%s' is in Maybe / Someday list now.") format(stuff.title.is))
        )
    }

    def saveScheduled(stuff: Stuff)(valueAttr: String): JsCmd = {

        val updatedStuff = updateStuff(stuff, StuffType.Scheduled)
        val action = Action.createRecord.idField(updatedStuff.idField.is)
        val scheduled = Scheduled.createRecord.idField(updatedStuff.idField.is)

        scheduled.startTime.setBox(startDateTime)
        scheduled.endTime.setBox(endDateTime)
        scheduled.location.setBox(location)
        scheduled.validate match {
            case Nil => 
                stuff.saveTheRecord()
                action.saveTheRecord()
                scheduled.saveTheRecord()
                S.redirectTo(
                    "/todo/process", 
                    () => S.notice(S.?("'%s' is in schedule now.") format(stuff.title.is))
                )

                Noop
            case xs  => Alert(xs.map(_.msg).mkString("、"))
        }
    }

    def getCalendarDate(dateString: String): Box[Calendar] = {
         optFromStr(dateString) match {
            case None => Empty
            case Some(date) => tryo {
                dateFormatter.setLenient(false)
                val date = dateFormatter.parse(dateString)
                val calendar = Calendar.getInstance
                calendar.setTime(date)
                calendar
            }
         }
    }

    def getCalendar(dateTimeString: String): Option[Calendar] = {
        optFromStr(dateTimeString) match {
            case None => None
            case Some(dateTimeString) =>
                try {
                    dateTimeFormatter.setLenient(false)
                    val dateTime = dateTimeFormatter.parse(dateTimeString)
                    val calendar = Calendar.getInstance
                    calendar.setTime(dateTime)
                    Some(calendar)
                } catch {
                    case e => None
                }
        }
    }

    def setDeadline(dateString: String): JsCmd = {

        val deadline = getCalendarDate(dateString)
        val errors = stuff.toList.flatMap { s => 
            s.deadline.setBox(deadline)
            s.deadline.validate
        }

        errors match {
            case Nil => 
                "$('#deadline_error').fadeOut()" &
                "$('#goToActionType').attr('disabled', false)"
            case xs  => 
                "$('#deadline_error').fadeIn()" &
                "$('#deadline_error_msg').text('%s')".format(xs.map(_.msg).mkString("、")) &
                "$('#goToActionType').attr('disabled', true)"
        }
    }


    def setEndDateTime(dateTimeString: String): JsCmd = {
        endDateTime = getCalendar(dateTimeString)
        Noop
    }

    def setStartDateTime(dateTimeString: String): JsCmd = {
        

        val dateTime = tryo {
            require(optFromStr(dateTimeString).isDefined, S.?("This field is required."))

            dateTimeFormatter.setLenient(false)

            val dateTime = dateTimeFormatter.parse(dateTimeString)
            val calendar = Calendar.getInstance
            calendar.setTime(dateTime)
            calendar
        }

        this.startDateTime = dateTime.toOption

        dateTime match {
            case Full(date) => 
                "$('#deadline_error').fadeOut()" &
                "$('#saveScheduled').attr('disabled', false)"
            case Empty => 
                "$('#deadline_error').fadeOut()" &
                "$('#saveScheduled').attr('disabled', true)"
            case Failure(msg, _, _) => 
                "$('#deadline_error').fadeIn()" &
                "$('#deadline_error_msg').text('%s')".format("SSSS") &
                "$('#saveScheduled').attr('disabled', true)"
        }
    }

    def setDescription(description: String): JsCmd = {
        this.description = description
        Noop
    }

    def setTickler(dateString: String): JsCmd = {

        this.tickler = getCalendarDate(dateString)
        val errors = stuff.map(x => Maybe.createRecord).toList.flatMap { maybe => 
            maybe.tickler.setBox(tickler)
            maybe.tickler.validate
        }


        errors match {
            case Nil => 
                "$('#tickler_error').fadeOut()" &
                "$('#saveMaybeTickler').attr('disabled', false)"
            case xs  => 
                "$('#tickler_error').fadeIn()" &
                "$('#tickler_error_msg').text('%s')".format(xs.map(_.msg).mkString("、")) &
                "$('#saveMaybeTickler').attr('disabled', true)"
        }
    }

    def hasStuffBinding(stuff: Stuff) = 
    {
        "#noStuffAlert" #> "" &
        "#isTrash [onclick]" #> SHtml.onEvent(markAsTrash(stuff)) &
        "#markAsDone [onclick]" #> SHtml.onEvent(markAsDone(stuff)) &
        "#location" #> SHtml.textAjaxTest("", doNothing _, location = _) &
        "#startDateTime" #> SHtml.textAjaxTest("", doNothing _, setStartDateTime _) &
        "#endDateTime" #> SHtml.textAjaxTest("", doNothing _, setEndDateTime _) &
        "#saveScheduled [onclick]" #> SHtml.onEvent(saveScheduled(stuff)) &
        "#processEdit" #> (
            createProjectTags("editFormProject") &
            createTopicTags("editFormTopic") &
            "#processTitle" #> ("input" #> SHtml.textAjaxTest(stuff.title.is, doNothing _, setTitle _)) &
            "#processEditDesc" #> SHtml.ajaxTextarea(description.getOrElse(""), setDescription _)
        ) &
        "#isNextAction" #> (
            createContextTags("nextActionContext") &
            "#saveNextAction [onclick]" #> SHtml.onEvent(saveNextAction(stuff))
        ) &
        "#isDelegated" #> (
            "#contactCombo" #> contactCombobox.comboBox &
            "#saveDelegated [onclick]" #> SHtml.onEvent(saveDelegated(stuff))
        ) &
        "#itIsReference" #> (
            createTopicTags("referenceTopic") &
            createProjectTags("referenceProject") &
            "#saveReference [onclick]" #> SHtml.onEvent(saveReference(stuff))
        ) &
        "#itIsMaybe" #> (
            createTopicTags("maybeTopic") &
            createProjectTags("maybeProject") &
            "#saveMaybe [onclick]" #> SHtml.onEvent(saveMaybe(stuff))
        ) &
        "#itIsMaybeTickler" #> (
            "#ticklerDate" #> SHtml.textAjaxTest("", doNothing _, setTickler _) &
            "#saveMaybeTickler [onclick]" #> SHtml.onEvent(saveMaybe(stuff))
        ) &
        "#whatIsNextAction" #> (
            createTopicTags("nextActionTopic") &
            createProjectTags("nextActionProject") &
            "#deadline" #> SHtml.textAjaxTest("", doNothing _, setDeadline _) &
            "name=nextActionTitle" #> 
                SHtml.textAjaxTest(stuff.title.is, doNothing _, setTitle _)
        )

    }

    def noStuffBinding = 
    {
        ".dialog" #> "" &
        ".editForm" #> ""
    }

    def render = {
        stuff match {
            case Some(stuff) => hasStuffBinding(stuff)
            case None        => noStuffBinding
        }
    }
}