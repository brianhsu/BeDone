package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import org.bedone.model.StuffType.StuffType

import net.liftweb.common._
import net.liftweb.util.Helpers._
import net.liftweb.util.FieldError
import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd

import java.util.Calendar


class Process extends ProjectTagger with TopicTagger with ContactTagger
              with ContextTagger with DeadlinePicker with TicklerPicker 
              with SchedulePicker with HasStuff with JSImplicit
{
    val currentUser = CurrentUser.is.get
    val stuff = Stuff.findByUser(currentUser).openOr(Nil)
                     .headOption

    override var currentProjects = stuff.map(_.projects).getOrElse(Nil)
    override var currentTopics = stuff.map(_.topics).getOrElse(Nil)
    override var currentContexts: List[Context] = Nil
    override var currentContact: Option[Contact] = None

    override val contextTagContainers = List("nextActionContext")
    override val projectTagContainers = List(
        "editFormProject", "referenceProject", 
        "maybeProject", "nextActionProject"
    )

    override val topicTagContainers = List(
        "editFormTopic", "referenceTopic", 
        "maybeTopic", "nextActionTopic"
    )


    // Stuff attirbute
    private var description: Option[String] = stuff.map(_.description.is)
    private var actionTitle: Option[String] = stuff.map(_.title.is)

    def setDeadline(dateString: String): JsCmd = 
    {
        val onOK: JsCmd = {
            "$('#deadline_error').fadeOut()" &
            "$('#goToActionType').attr('disabled', false)"
        }
        
        def onError(xs: List[FieldError]): JsCmd = {
            "$('#deadline_error').fadeIn()" &
            "$('#deadline_error_msg').text('%s')".format(xs.map(_.msg).mkString("、")) &
            "$('#goToActionType').attr('disabled', true)"
        }

        super.setDeadline(dateString, onOK, onError _)
    }

    def setDescription(description: String): JsCmd = {
        this.description = description
        Noop
    }

    def setTickler(dateString: String) =
    {
        val onOK: JsCmd = {
            "$('#tickler_error').fadeOut()" &
            "$('#saveMaybeTickler').attr('disabled', false)"
        }

        def onError(error: List[FieldError]): JsCmd = {
            "$('#tickler_error').fadeIn()" &
            "$('#tickler_error_msg').text('%s')".format(error.map(_.msg).mkString("、")) &
            "$('#saveMaybeTickler').attr('disabled', true)"
        }

        super.setTickler(dateString, onOK, onError)
    }

    def setEndTime(dateTimeString: String): JsCmd =
    {
        def onOK: JsCmd = {
            
            val isScheduleValid = !validateSchedule.isEmpty

            "$('#endTime_error').fadeOut()" &
            "$('#saveScheduled').attr('disabled', %s)".format(isScheduleValid)
        }

        def onError(error: List[FieldError]): JsCmd = {
            "$('#endTime_error').fadeIn()" &
            "$('#endTime_error_msg').text('%s')".format(error.flatMap(_.msg).mkString) &
            "$('#saveScheduled').attr('disabled', true)"
        }

        super.setEndTime(dateTimeString, onOK, onError)

    }

    def setStartTime(dateTimeString: String): JsCmd = 
    {
        def onOK: JsCmd = {
            val isScheduleValid = !validateSchedule.isEmpty

            "$('#startTime_error').fadeOut()" &
            "$('#saveScheduled').attr('disabled', %s)".format(isScheduleValid)
        }

        def onError(error: List[FieldError]): JsCmd = {
            "$('#startTime_error').fadeIn()" &
            "$('#startTime_error_msg').text('%s')".format(error.flatMap(_.msg).mkString) &
            "$('#saveScheduled').attr('disabled', true)"
        }

        super.setStartTime(dateTimeString, onOK, onError)
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

        scheduled.startTime.setBox(startTime)
        scheduled.endTime.setBox(endTime)
        scheduled.location.setBox(location)
        stuff.saveTheRecord()
        action.saveTheRecord()
        scheduled.saveTheRecord()

        S.redirectTo(
            "/todo/process", 
            () => S.notice(S.?("'%s' is in schedule now.") format(stuff.title.is))
        )

        Noop
    }

    def hasStuffBinding(stuff: Stuff) = 
    {

        val noContactSelected =  """$('#saveDelegated').attr('disabled', true)"""
        val contactSelected = """$('#saveDelegated').attr('disabled', false)"""

        "#noStuffAlert" #> "" &
        "#isTrash [onclick]" #> SHtml.onEvent(markAsTrash(stuff)) &
        "#markAsDone [onclick]" #> SHtml.onEvent(markAsDone(stuff)) &
        "#location" #> SHtml.textAjaxTest("", doNothing _, setLocation _) &
        "#startTime" #> SHtml.textAjaxTest("", doNothing _, setStartTime _) &
        "#endTime" #> SHtml.textAjaxTest("", doNothing _, setEndTime _) &
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
            "#contactCombo" #> contactCombobox(noContactSelected, contactSelected) &
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
