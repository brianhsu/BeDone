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

class NextActionModalHelper(stuffID: Int) extends ProjectTagger with TopicTagger 
                      with ContextTagger with DeadlinePicker 
                      with HasStuff with JSImplicit
{
    val stuff = Stuff.findByID(stuffID.toInt).toOption

    override val projectTagContainers = List("nextActionProject")
    override val topicTagContainers = List("nextActionTopic")
    override val contextTagContainers = List("nextActionContext")

    override var currentProjects = stuff.map(_.projects).getOrElse(Nil)
    override var currentTopics = stuff.map(_.topics).getOrElse(Nil)
    override var currentContexts: List[Context] = Nil

    // Stuff attirbute
    private var description: Option[String] = stuff.map(_.description.is)
    private var actionTitle: Option[String] = stuff.map(_.title.is)

    def doNothing(s: String) {}

    def setDescription(description: String): JsCmd = {
        this.description = description
        Noop
    }

    def setTitle(title: String): JsCmd = {

        val stuffTitle = stuff.map(_.title.is).getOrElse("")

        this.actionTitle = title
        this.actionTitle match {
            case None => "$('#actionTitle').val('%s');".format(stuffTitle)
            case Some(text) => Noop
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

    def saveNextAction(valueAttr: String): JsCmd = {
        
        val resultJS = stuff.map { todo =>
            val updatedStuff = updateStuff(todo, StuffType.Action)
            val action = Action.createRecord.idField(updatedStuff.idField.is)

            action.saveTheRecord()
            action.setContexts(currentContexts)

            """$('#nextActionModal').modal('hide')""" &
            RemoveInboxRow(todo.idField.is.toString) &
            """updatePaging()"""
        }

        resultJS.getOrElse(Noop)
    }

    def setDeadline(dateString: String): JsCmd = 
    {
        val onOK: JsCmd = {
            "$('#deadline_error').fadeOut()" &
            "$('#saveButton').attr('disabled', false)"
        }

        def onError(xs: List[FieldError]): JsCmd = {
            "$('#deadline_error').fadeIn()" &
            "$('#deadline_error_msg').text('%s')".format(xs.map(_.msg).mkString("、")) &
            "$('#saveButton').attr('disabled', true)"
        }

        super.setDeadline(dateString, onOK, onError)
    }

}


class NextActionModal
{
    def render = {

        val stuffID = S.attr("stuffID").map(_.toInt).openOrThrowException("No stuffID")
        val stuff = Stuff.findByID(stuffID.toInt)
        val stuffTitle = stuff.map(_.title.is).getOrElse("")

        val helper = new NextActionModalHelper(stuffID)

        helper.createProjectTags("nextActionProject") &
        helper.createTopicTags("nextActionTopic") &
        helper.createContextTags("nextActionContext") &
        "#actionTitle" #> SHtml.textAjaxTest(
            stuffTitle, helper.doNothing _, helper.setTitle _
        ) &
        "#actionDesc" #> SHtml.ajaxTextarea(
            stuff.map(_.description.is).getOrElse(""), 
            helper.setDescription _
        ) &
        "#deadline" #> SHtml.textAjaxTest("", helper.doNothing _, helper.setDeadline _) &
        "#saveButton [onclick]" #> SHtml.onEvent(helper.saveNextAction _)
    }
}
