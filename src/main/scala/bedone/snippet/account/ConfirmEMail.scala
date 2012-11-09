package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmd

import net.liftweb.util.Helpers._
import net.liftweb.util.PassThru

import java.util.Calendar
import java.util.Date

class ConfirmEMail
{
    def render = {
        S.notice("Hello World")

        PassThru
    }
}
