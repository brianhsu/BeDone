package org.bedone.snippet

import org.bedone.session._
import org.bedone.model._

import net.liftweb.util.Helpers._
import net.liftweb.http.S
import net.liftweb.http.SHtml

import org.scribe.builder._
import org.scribe.model._
import org.scribe.builder.api._

import scala.xml.XML


class ContactsImport
{
    private lazy val contacts = getContacts
    private val currentUser = CurrentUser.get.get
    private var shouldBeSaved = contacts.toSet

    def getContacts = {
        ContactsOAuth.is.flatMap { api =>
            api.setAccessToken(S.param("oauth_verifier").openOr(""))
            api.contacts
        }.openOr(Nil).sortBy(_.name.is)
    }

    def updateImportSet (contact: Contact, isChecked: Boolean) = {
        shouldBeSaved = isChecked match {
            case true  => shouldBeSaved + contact
            case false => shouldBeSaved - contact
        }
    }

    def importContacts() {
        shouldBeSaved.foreach { contact => 
            contact.userID(currentUser.idField.is)
            println(contact.saveTheRecord())
        }

        S.redirectTo("/contact", () => S.notice("已匯入 GMail 通訊錄"))
    }

    def render = {

        if (contacts.isEmpty) {
            S.redirectTo("/contact", () => S.notice("無可匯入的資料"))
        }

        ".backButton [onclick]" #> SHtml.onEvent(s => S.redirectTo("/contact")) &
        ".confirmButton [onclick]" #> SHtml.onEvent(s => importContacts()) &
        ".contactRow" #> contacts.map { contact =>
            ".save *" #> SHtml.ajaxCheckbox(true, updateImportSet(contact, _)) &
            ".name *" #> contact.name.is &
            ".email *" #> contact.email.is &
            ".phone *" #> contact.phone.is &
            ".address *" #> contact.address.is
        }
    }
}
