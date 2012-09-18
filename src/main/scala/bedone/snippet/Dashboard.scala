package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftmodules.combobox._
import net.liftweb.common.Box

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import java.text.SimpleDateFormat

class DashBoard
{
    private val currentUser = CurrentUser.get.get
    def render = {

        def onItemSelected(item: Option[ComboItem]): JsCmd = {
            println("onItemSelected:" + item)
        }

        def onItemAdded(text: String): JsCmd = {
            println("onItemAdded:" + text)
        }

        def onSearching(term: String) = {
            Contact.findByUser(currentUser).openOr(Nil)
                   .filter(_.name.is.contains(term))
                   .map(x => ComboItem(x.idField.is.toString, x.name.is))
        }

        val jsonOption = List(
            ("placeholder" -> "請選擇負責人"),
            ("allowClear" -> "true")
        )

        val comboBox = ComboBox(None, onSearching _, onItemSelected _, onItemAdded _, jsonOption)


        "name=contactInput" #> comboBox.comboBox
    }
}
