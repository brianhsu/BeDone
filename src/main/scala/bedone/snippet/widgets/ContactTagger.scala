package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds._

import TagButton.Implicit._

trait ContactTagger extends JSImplicit 
{
    var currentContact: Option[Contact]

    def contactCombobox(notSelect: JsCmd, onSelect: JsCmd) = new ContactComboBox {
        override def setContact(selected: Option[Contact]): JsCmd = {
            selected match {
                case None => 
                    currentContact = None
                    notSelect
                case Some(contact) => 
                    currentContact = selected
                    onSelect
            }
        }
    }.comboBox
}

