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

class EditContactForm(contact: Contact, postAction: Contact => JsCmd) extends JSImplicit
{
    val currentUser = CurrentUser.is.get

    private def optFromStr(str: String) = Option(str).filterNot(_.trim.length == 0)
    private def template = Templates("templates-hidden" :: "contact" :: "edit" :: Nil)

    private var name: String = contact.name.is
    private var email: Option[String] = contact.email.is
    private var phone: Option[String] = contact.phone.is
    private var address: Option[String] = contact.address.is

    def saveContact(): JsCmd = {
        contact.saveTheRecord()
        postAction(contact)
    }

    def updateContact(): JsCmd = {

        contact.name(name).email(email)
               .phone(phone).address(address)

        contact.validate match {
            case Nil    => saveContact()
            case errors => S.error(errors.map(_.msg).mkString("、"))
        }
    }

    def setName(name: String) { this.name = name }
    def setEmail(email: String) { this.email = optFromStr(email) }
    def setPhone(phone: String) { this.phone = optFromStr(phone) }
    def setAddress(address: String) { this.address = optFromStr(address) }

    def validateName(name: String): JsCmd = optFromStr(name) match {
        case None    => "$('#contactEditSave').attr('disabled', true)"
        case Some(t) => "$('#contactEditSave').attr('disabled', false)"
    }

    def cssBinder = {
        "#contactEditName"    #> SHtml.textAjaxTest(name, setName _, validateName _) &
        "#contactEditEmail"   #> SHtml.text(email.getOrElse(""), setEmail _) &
        "#contactEditPhone"   #> SHtml.text(phone.getOrElse(""), setPhone _) &
        "#contactEditAddress" #> SHtml.text(address.getOrElse(""), setAddress _) &
        "#contactEditHidden"  #> SHtml.hidden(updateContact) &
        "#contactEditCancel [onclick]" #> (
            FadeOutAndRemove("editContactForm") & "return false"
        )

    }

    def toForm = {
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }
}

