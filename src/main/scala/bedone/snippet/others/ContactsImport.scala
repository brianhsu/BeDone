package org.bedone.snippet

import org.bedone.lib._
import org.bedone.session._
import org.bedone.model._

import net.liftweb.util.Helpers._
import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE.JsRaw

import org.scribe.builder._
import org.scribe.model._
import org.scribe.builder.api._

import scala.xml.XML


class ContactsImport extends JSImplicit
{
    private lazy val contacts = getContacts
    private val currentUser = CurrentUser.get.get
    private var shouldBeSaved = contacts.toSet

    def getContacts = {

        ContactsOAuth.is.flatMap { api =>
            api.setAccessToken(S.param("code").openOr(""))
            api.contacts
        }.openOr(Nil).sortBy(_.name.is.toLowerCase)
    }

    def updateImportSet (contact: Contact, isChecked: Boolean): JsCmd = {
        shouldBeSaved = isChecked match {
            case true  => shouldBeSaved + contact
            case false => shouldBeSaved - contact
        }

        if (shouldBeSaved.size == contacts.size) {
            """$("#selectAll").attr('checked', true)"""
        } else {
            """$("#selectAll").attr('checked', false)"""
        }
    }

    def importContacts() {
        shouldBeSaved.foreach { contact => 
            contact.userID(currentUser.idField.is).isTrash(false).saveTheRecord()
        }

        S.redirectTo("/contact/", () => S.notice(S.?("GMail contacts imported successfully.")))
    }

    def toogleAll(isChecked: Boolean): JsCmd = {
        isChecked match {
            case true  => shouldBeSaved = contacts.toSet
            case false => shouldBeSaved = Set()
        }

        """
            $('input[name="save"]').each(function() {
                $(this).attr('checked', %s);
            });
        """ format(isChecked)
    }

    def render = {


        "#selectAll" #> SHtml.ajaxCheckbox(true, toogleAll _) &
        ".backButton [onclick]" #> SHtml.onEvent(s => S.redirectTo("/contact/")) &
        ".confirmButton [onclick]" #> SHtml.onEvent(s => importContacts()) &
        ".contactRow" #> contacts.map { contact =>
            ".save" #> SHtml.ajaxCheckbox(true, updateImportSet(contact, _)) &
            ".name *" #> contact.name.is &
            ".email *" #> contact.email.is.getOrElse("") &
            ".phone *" #> contact.phone.is.getOrElse("") &
            ".address *" #> contact.address.is.getOrElse("")
        }
    }
}
