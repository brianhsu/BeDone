package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import java.text.SimpleDateFormat

class ContactTable extends JSImplicit
{
    val currentUser = CurrentUser.is.get
    def contacts = Contact.findByUser(currentUser).openOr(Nil).filterNot(_.isTrash.is)

    def deleteContact(contact: Contact)(value: String) = {
        contact.isTrash(true).saveTheRecord()
        FadeOutAndRemove("contact" + contact.idField.is)
    }

    def createContactRow(contact: Contact) = {
        ".contactRow [id]" #> ("contact" + contact.idField.is) &
        ".name *" #> contact.name.is &
        ".email *" #> contact.email.is.getOrElse("") &
        ".phone *" #> contact.phone.is.getOrElse("") &
        ".address *" #> contact.address.is.getOrElse("") &
        ".delete [onclick]" #> SHtml.onEvent(deleteContact(contact))
    }

    def render = {
        ".contactRow" #> contacts.map(createContactRow)
    }
}
