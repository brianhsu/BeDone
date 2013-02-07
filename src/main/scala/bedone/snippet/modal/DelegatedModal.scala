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

class DelegatedModalHelper(stuffID: Int) extends ProjectTagger with TopicTagger 
                                         with ContactTagger with HasStuff with JSImplicit
{
    val stuff = S.attr("stuffID")
                 .flatMap(stuffID => Stuff.findByID(stuffID.toInt))
                 .toOption

    override val projectTagContainers = List("delegatedProject")
    override val topicTagContainers = List("delegatedTopic")

    override var currentProjects = stuff.map(_.projects).getOrElse(Nil)
    override var currentTopics = stuff.map(_.topics).getOrElse(Nil)
    override var currentContact: Option[Contact] = None

    private var description: Option[String] = stuff.map(_.description.is)
    private var delegatedTitle: Option[String] = stuff.map(_.title.is)

    def doNothing(s: String) {}

    def setDescription(description: String): JsCmd = {
        this.description = description
        Noop
    }

    def setTitle(title: String): JsCmd = {

        val stuffTitle = stuff.map(_.title.is).getOrElse("")

        this.delegatedTitle = title
        this.delegatedTitle match {
            case None => "$('#delegatedTitle').val('%s');".format(stuffTitle)
            case Some(text) => Noop
        }
    }

    def updateStuff(stuff: Stuff, stuffType: StuffType) =
    {
        stuff.title(delegatedTitle.getOrElse(stuff.title.is))
        stuff.stuffType(stuffType)
        stuff.setProjects(currentProjects)
        stuff.setTopics(currentTopics)
        stuff.description(description.getOrElse(""))
        stuff.saveTheRecord()
        stuff
    }

    def saveDelegated(valueAttr: String): JsCmd = {

        for (contact <- currentContact; todo <- stuff) {

            contact.saveTheRecord()

            val stuffID = todo.idField.is
            val updatedStuff = updateStuff(todo, StuffType.Delegated)
            val action = Action.createRecord.idField(stuffID).saveTheRecord().get

            Delegated.createRecord.idField(stuffID)
                     .contactID(contact.idField.is)
                     .saveTheRecord()
        }

        """$('#delegatedModal').modal('hide')""" &
        RemoveInboxRow(stuff.map(_.idField.is.toString).getOrElse("")) &
        """updatePaging()"""
    }

}

class DelegatedModal extends JSImplicit
{

    def render = {

        val stuffID = S.attr("stuffID").map(_.toInt).openOrThrowException("No stuffID")
        val stuff = Stuff.findByID(stuffID.toInt)
        val stuffTitle = stuff.map(_.title.is).getOrElse("")
        val helper = new DelegatedModalHelper(stuffID)

        val noContactSelected =  """$('#saveButton').attr('disabled', true)"""
        val contactSelected = """$('#saveButton').attr('disabled', false)"""

        helper.createProjectTags("delegatedProject") &
        helper.createTopicTags("delegatedTopic") &
        "#contactCombo" #> helper.contactCombobox(noContactSelected, contactSelected) &
        "#delegatedTitle" #> SHtml.textAjaxTest(
            stuffTitle, helper.doNothing _, helper.setTitle _
        ) &
        "#delegatedDesc" #> SHtml.ajaxTextarea(
            stuff.map(_.description.is).getOrElse(""), 
            helper.setDescription _
        ) &
        "#saveButton [onclick]" #> SHtml.onEvent(helper.saveDelegated)
    }
}
