package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._
import org.bedone.session._

import net.liftweb.common._

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import java.text.SimpleDateFormat

class ContactTable extends JSImplicit
{
    val rowTemplate = Templates("templates-hidden" :: "contact" :: "item" :: Nil)
    val currentUser = CurrentUser.is.get
    def contacts = Contact.findByUser(currentUser).openOr(Nil).filterNot(_.isTrash.is)

    var selected: Set[Contact] = Set()

    def editContact(contact: Contact): JsCmd = {

        def callback(contact: Contact) = {
            val rowID = "contact" + contact.idField.is

            FadeOutAndRemove("editContactForm") &
            Replace(rowID, createContactRow(contact))
        }

        val editForm = new EditContactForm(contact, callback)
        SetHtml("editContactHolder", editForm.toForm)
    }

    def deleteContact(contact: Contact) = {
        contact.isTrash(true).saveTheRecord()

        S.notice("已將「%s」放入垃圾桶" format(contact.name.is))
        FadeOutAndRemove("contact" + contact.idField.is)
    }

    def select(contact: Contact)(checked: Boolean): JsCmd = {
        this.selected = checked match {
            case true  => selected + contact
            case false => selected - contact
        }

        val updateDeleteButton = this.selected.size match {
            case 0 => """$('#deleteSelected').hide()"""
            case _ => """$('#deleteSelected').show()"""
        }

        val updateSelectAll = (this.selected.size == this.contacts.size) match {
            case true  => """$('#selectAll').attr('checked', true)"""
            case false => """$('#selectAll').attr('checked', false)"""
        }

        updateDeleteButton & updateSelectAll
    }

    def createContactRow(contact: Contact) = {
        
        val cssBinding = 
            ".contactRow [id]"  #> ("contact" + contact.idField.is) &
            ".select"           #> SHtml.ajaxCheckbox(false, select(contact)) &
            ".name *"           #> contact.name.is &
            ".email *"          #> contact.email.is.getOrElse("") &
            ".email [href]"     #> contact.email.is.map("mailto:" + _).getOrElse("#") &
            ".phone *"          #> contact.phone.is.getOrElse("") &
            ".address *"        #> contact.address.is.getOrElse("") &
            ".edit [onclick]"   #> SHtml.onEvent(s => editContact(contact)) &
            ".delete [onclick]" #> SHtml.onEvent(s => deleteContact(contact)) &
            ".detail [href]"    #> ("/contact/" + contact.idField.is)

        rowTemplate.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def gmailAuth() {
        val contactsOAuth = new GMailContacts
        ContactsOAuth(Full(contactsOAuth))
        S.redirectTo(contactsOAuth.authURL)
    }

    def toggleSelectAll(isChecked: Boolean): JsCmd = {

        this.selected = isChecked match {
            case true  => contacts.toSet
            case false => Set()
        }

        val showOrHide = isChecked match {
            case true => """$('#deleteSelected').show()"""
            case false => """$('#deleteSelected').hide()"""
        }


        showOrHide &
        """
            $('.select').each(function() {
                $(this).attr('checked', %s);
            });
        """.format(isChecked)

    }

    def deleteSelected(): JsCmd = {

        val deleteJS = selected.map { contact => 
            contact.isTrash(true).saveTheRecord()
            FadeOutAndRemove("contact" + contact.idField.is)
        }

        """
          $('#selectAll').attr('checked', false);
          $('#deleteSelected').hide();
        """ & deleteJS.toList
    }

    def render = {
        "#selectAll" #> SHtml.ajaxCheckbox(false, toggleSelectAll _) &
        "#importContact [onclick]"  #> SHtml.onEvent(s => gmailAuth()) &
        "#deleteSelected [onclick]" #> SHtml.onEvent(s => deleteSelected()) &
        ".contactRow" #> contacts.map(createContactRow)
    }
}
