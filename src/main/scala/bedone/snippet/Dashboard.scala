package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

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
    def render = {
        
        val comboBox = new ComboBox("http://localhost:8081/autocomplete/contact", true) {
            override def onItemSelected(id: String, text: String): JsCmd = {
                println("onItemSelected:" + (id, text))
            }

            override def onItemAdded(text: String): JsCmd = {
                println("onItemAdded:" + text)
            }

        }

        "name=contactInput" #> comboBox.comboBox
    }
}
