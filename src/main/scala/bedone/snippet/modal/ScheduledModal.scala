package org.bedone.snippet

import org.bedone.model._
import org.bedone.model.StuffType.StuffType
import org.bedone.lib._

import net.liftweb.http.S
import net.liftweb.http.SHtml

import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._

import net.liftweb.util.Helpers._
import net.liftweb.util.FieldError

class ScheduledModalHelper(stuffID: Int) extends ProjectTagger with TopicTagger 
                                         with SchedulePicker with HasStuff with JSImplicit
{
    val stuff = S.attr("stuffID")
                 .flatMap(stuffID => Stuff.findByID(stuffID.toInt))
                 .toOption

    override val projectTagContainers = List("scheduledProject")
    override val topicTagContainers = List("scheduledTopic")

    override var currentProjects = stuff.map(_.projects).getOrElse(Nil)
    override var currentTopics = stuff.map(_.topics).getOrElse(Nil)

    private var description: Option[String] = stuff.map(_.description.is)
    private var scheduledTitle: Option[String] = stuff.map(_.title.is)

    def doNothing(s: String) {}

    def setDescription(description: String): JsCmd = {
        this.description = description
        Noop
    }

    def setTitle(title: String): JsCmd = {

        val stuffTitle = stuff.map(_.title.is).getOrElse("")

        this.scheduledTitle = title
        this.scheduledTitle match {
            case None => "$('#scheduledTitle').val('%s');".format(stuffTitle)
            case Some(text) => Noop
        }
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
            "$('#saveButton').attr('disabled', true)"
        }

        super.setStartTime(dateTimeString, onOK, onError)
    }

    def setEndTime(dateTimeString: String): JsCmd =
    {
        def onOK: JsCmd = {
            
            val isScheduleValid = !validateSchedule.isEmpty

            "$('#endTime_error').fadeOut()" &
            "$('#saveButton').attr('disabled', %s)".format(isScheduleValid)
        }

        def onError(error: List[FieldError]): JsCmd = {
            "$('#endTime_error').fadeIn()" &
            "$('#endTime_error_msg').text('%s')".format(error.flatMap(_.msg).mkString) &
            "$('#saveButton').attr('disabled', true)"
        }

        super.setEndTime(dateTimeString, onOK, onError)
    }

    def updateStuff(stuff: Stuff, stuffType: StuffType) =
    {
        stuff.title(scheduledTitle.getOrElse(stuff.title.is))
        stuff.stuffType(stuffType)
        stuff.setProjects(currentProjects)
        stuff.setTopics(currentTopics)
        stuff.description(description.getOrElse(""))
        stuff.saveTheRecord()
        stuff
    }

    def saveScheduled(valueAttr: String): JsCmd = 
    {
        stuff.map { todo =>

            val updatedStuff = updateStuff(todo, StuffType.Scheduled)
            val action = Action.createRecord.idField(updatedStuff.idField.is)
            val scheduled = Scheduled.createRecord.idField(updatedStuff.idField.is)

            action.saveTheRecord()
            scheduled.startTime.setBox(startTime)
            scheduled.endTime.setBox(endTime)
            scheduled.location.setBox(location)
            scheduled.saveTheRecord()

            """$('#scheduledModal').modal('hide')""" &
            RemoveInboxRow(todo.idField.is.toString) &
            """updatePaging()"""

        }.toList
    }

}

class ScheduledModal 
{

    def render = {

        val stuffID = S.attr("stuffID").map(_.toInt).openOrThrowException("No stuffID")
        val stuff = Stuff.findByID(stuffID.toInt)
        val stuffTitle = stuff.map(_.title.is).getOrElse("")
        val helper = new ScheduledModalHelper(stuffID)

        helper.createProjectTags("scheduledProject") &
        helper.createTopicTags("scheduledTopic") &
        "#location" #> SHtml.textAjaxTest("", helper.doNothing _, helper.setLocation _) &
        "#startTime" #> SHtml.textAjaxTest("", helper.doNothing _, helper.setStartTime _) &
        "#endTime" #> SHtml.textAjaxTest("", helper.doNothing _, helper.setEndTime _) &
        "#scheduledTitle" #> SHtml.textAjaxTest(
            stuffTitle, helper.doNothing _, helper.setTitle _
        ) &
        "#scheduledDesc" #> SHtml.ajaxTextarea(
            stuff.map(_.description.is).getOrElse(""), 
            helper.setDescription _
        ) &
        "#saveButton [onclick]" #> SHtml.onEvent(helper.saveScheduled)
    }
}
