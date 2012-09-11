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

class Inbox extends StuffList with JSImplicit
{
    val projectID: Box[Int] = S.attr("projectID").map(_.toInt)

    def render = {
        cssBinding
    }
}
