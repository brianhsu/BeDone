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

class MaybeModalHelper(stuffID: Int) extends ProjectTagger with TopicTagger 
                                     with TicklerPicker with HasStuff with JSImplicit
{
    val stuff = S.attr("stuffID")
                 .flatMap(stuffID => Stuff.findByID(stuffID.toInt))
                 .toOption

    override val projectTagContainers = List("maybeProject")
    override val topicTagContainers = List("maybeTopic")

    override var currentProjects = stuff.map(_.projects).getOrElse(Nil)
    override var currentTopics = stuff.map(_.topics).getOrElse(Nil)

    private var description: Option[String] = stuff.map(_.description.is)
    private var maybeTitle: Option[String] = stuff.map(_.title.is)

    def doNothing(s: String) {}

    def setDescription(description: String): JsCmd = {
        this.description = description
        Noop
    }

    def setTitle(title: String): JsCmd = {

        val stuffTitle = stuff.map(_.title.is).getOrElse("")

        this.maybeTitle = title
        this.maybeTitle match {
            case None => "$('#maybeTitle').val('%s');".format(stuffTitle)
            case Some(text) => Noop
        }
    }

    def updateStuff(stuff: Stuff, stuffType: StuffType) =
    {
        stuff.title(maybeTitle.getOrElse(stuff.title.is))
        stuff.stuffType(stuffType)
        stuff.setProjects(currentProjects)
        stuff.setTopics(currentTopics)
        stuff.description(description.getOrElse(""))
        stuff.saveTheRecord()
        stuff
    }

    def setTickler(dateString: String) =
    {
        val onOK: JsCmd = {
            "$('#tickler_error').fadeOut()" &
            "$('#saveButton').attr('disabled', false)"
        }

        def onError(error: List[FieldError]): JsCmd = {
            "$('#tickler_error').fadeIn()" &
            "$('#tickler_error_msg').text('%s')".format(error.map(_.msg).mkString("、")) &
            "$('#saveButton').attr('disabled', true)"
        }

        super.setTickler(dateString, onOK, onError)
    }

    def saveMaybe(valueAttr: String): JsCmd = {

        stuff.map { todo =>
            val updatedStuff = updateStuff(todo, StuffType.Maybe)
            val maybe = Maybe.createRecord.idField(todo.idField.is)

            maybe.tickler(this.tickler.toOption)
            maybe.saveTheRecord()

            """$('#maybeModal').modal('hide')""" &
            RemoveInboxRow(todo.idField.is.toString) &
            """updatePaging()"""
        }.toList

    }
}

class MaybeModal 
{
    def render = {

        val stuffID = S.attr("stuffID").map(_.toInt).openOrThrowException("No stuffID")
        val stuff = Stuff.findByID(stuffID.toInt)
        val stuffTitle = stuff.map(_.title.is).getOrElse("")
        val helper = new MaybeModalHelper(stuffID)

        helper.createProjectTags("maybeProject") &
        helper.createTopicTags("maybeTopic") &
        "#tickler" #> SHtml.textAjaxTest("", helper.doNothing _, helper.setTickler _) &
        "#maybeTitle" #> SHtml.textAjaxTest(
            stuffTitle, helper.doNothing _, helper.setTitle _
        ) &
        "#maybeDesc" #> SHtml.ajaxTextarea(
            stuff.map(_.description.is).getOrElse(""), 
            helper.setDescription _
        ) &
        "#saveButton [onclick]" #> SHtml.onEvent(helper.saveMaybe)

    }
}
