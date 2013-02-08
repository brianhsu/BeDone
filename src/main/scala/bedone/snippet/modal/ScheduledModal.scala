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
                                         with HasStuff with JSImplicit
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
        "#scheduledTitle" #> SHtml.textAjaxTest(
            stuffTitle, helper.doNothing _, helper.setTitle _
        ) &
        "#scheduledDesc" #> SHtml.ajaxTextarea(
            stuff.map(_.description.is).getOrElse(""), 
            helper.setDescription _
        )
    }
}
