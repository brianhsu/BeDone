package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

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

class EditTopicForm(topic: Topic, postAction: Topic => JsCmd) extends JSImplicit
{
    private def optFromStr(str: String) = Option(str).filterNot(_.trim.length == 0)
    private def template = Templates("templates-hidden" :: "topic" :: "edit" :: Nil)

    private var title: String = topic.title.is
    private var description: String = topic.description.is

    def setTitle(title: String) { this.title = title }

    def updateTopic(): JsCmd = {

        topic.title(title).description(description)
             .saveTheRecord()

        postAction(topic)
    }

    def validateTitle(title: String): JsCmd = optFromStr(title) match {
        case None    => "$('#topicEditSave').attr('disabled', true)"
        case Some(t) => "$('#topicEditSave').attr('disabled', false)"
    }

    def cssBinder = {
        "#topicEditTitle" #> SHtml.textAjaxTest(title, setTitle _, validateTitle _) &
        "#topicEditDesc" #> SHtml.textarea(description, description = _) &
        "#topicEditHidden" #> SHtml.hidden(updateTopic) &
        "#topicEditCancel [onclick]" #> (
            FadeOutAndRemove("editTopicForm") & "return false"
        )
    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

