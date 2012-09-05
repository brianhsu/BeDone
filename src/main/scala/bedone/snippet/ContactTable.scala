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

class ContactTable
{
    val currentUser = CurrentUser.is.get
    def contacts = Contact.findByUser(currentUser).openOr(Nil).filterNot(_.isTrash.is)

    def createContactRow(contact: Contact) = {
        ".contactRow [id]" #> ("contact" + contact.idField.is) &
        ".name *" #> contact.name.is &
        ".email *" #> contact.email.is &
        ".phone *" #> contact.phone.is &
        ".address *" #> contact.address.is
    }

    def render = {
        ".contactRow" #> contacts.map(createContactRow)
    }
}
