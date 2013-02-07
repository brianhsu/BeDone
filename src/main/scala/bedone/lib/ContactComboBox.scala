package org.bedone.lib

import org.bedone.model._

import net.liftmodules.combobox._

import net.liftweb.http.S
import net.liftweb.http.js.JE.Str
import net.liftweb.http.js.JsExp
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd

object ContactComboBox {
    def defaultOptions = List(
        "placeholder" -> Str(
            """<i class="icon-user"></i> """ + S.?("Who should work on this?")
        ),
        "width" -> Str("500px")
    )
}

abstract class ContactComboBox(options: List[(String, JsExp)] = ContactComboBox.defaultOptions) extends ComboBox(None, true, options) {
    
    private lazy val currentUser = CurrentUser.get.get

    def setContact(contact: Option[Contact]): JsCmd

    override def onSearching(term: String): List[ComboItem] = {
        
        def matchNameOrEMail(contact: Contact) = {
            contact.name.is.contains(term) ||
            contact.email.is.map(_.contains(term)).getOrElse(false)
        }

        def createComboItem(contact: Contact) = {
            val displayName = contact.email.is match {
                case None => contact.name.is
                case Some(address) => "%s 《%s》" format(contact.name.is, address)
            }

            ComboItem(contact.idField.toString, displayName)
        }

        
        Contact.findByUser(currentUser).openOr(Nil)
               .filter(matchNameOrEMail)
               .map(createComboItem).toList
    }

    override def onItemSelected(item: Option[ComboItem]): JsCmd = {
        item.map(i => setContact(Contact.findByID(i.id.toInt).toOption)).getOrElse(Noop)
    }

    override def onItemAdded(name: String): JsCmd = {
        val newContact = Contact.createRecord.name(name).userID(currentUser.idField.is)
        setContact(Some(newContact))
    }
}

