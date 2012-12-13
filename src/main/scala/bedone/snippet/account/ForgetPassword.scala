package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common._

import net.liftweb.http.js.JsCmds._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmd

import net.liftweb.util.Helpers._
import net.liftweb.util.PassThru

import java.util.Calendar
import java.util.Date

class ForgetPassword extends JSImplicit
{
    private var email: String = _

    private def optFromStr(str: String) = Option(str).filterNot(_.trim.length == 0)

    def process: JsCmd = {
        
        val userBox = User.findByEmail(email)

        userBox match {
            case Full(user) => 
                user.resetActivationCode(ActivationStatus.Reset)
                user.saveTheRecord()
                S.notice(S.?("We've send you an EMail with a password reset link, please check your email inbox and follow the instruction in it."))
                FadeOutAndRemove("forgetForm")
            case _ => 
                S.error(S.?("Sorry, we can't find this email in our system."))
                Noop
        }

    }

    def render = {
        "name=email" #> SHtml.text("", email = _) &
        "type=submit" #> SHtml.ajaxSubmit(S.?("Submit"), process _)
    }
}
