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

class EditProjectForm(project: Project, postAction: Project => JsCmd) extends JSImplicit
{
    private def optFromStr(str: String) = Option(str).filterNot(_.trim.length == 0)
    private def template = Templates("templates-hidden" :: "project" :: "edit" :: Nil)

    private var title: String = project.title.is
    private var description: String = project.description.is

    def setTitle(title: String) { this.title = title }

    def updateProject(): JsCmd = {

        project.title(title).description(description)
               .saveTheRecord()

        postAction(project)
    }

    def validateTitle(title: String): JsCmd = optFromStr(title) match {
        case None    => "$('#projectEditSave').attr('disabled', true)"
        case Some(t) => "$('#projectEditSave').attr('disabled', false)"
    }

    def cssBinder = {
        "#projectEditTitle" #> SHtml.textAjaxTest(title, setTitle _, validateTitle _) &
        "#projectEditDesc" #> (
            SHtml.textarea(description, description = _) ++ SHtml.hidden(updateProject)
        ) &
        "#projectEditCancel [onclick]" #> (
            FadeOutAndRemove("editProjectForm") & "return false"
        )
    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

