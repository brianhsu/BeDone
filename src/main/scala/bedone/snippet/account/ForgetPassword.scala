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
                S.notice("已將重設密碼的的連結寄至您的信箱，請使用信內的連結重設密碼") 
                FadeOutAndRemove("forgetForm")
            case _ => 
                S.error("抱歉，無法找到此 Email 的帳號")
                Noop
        }

    }

    def render = {
        "name=email" #> SHtml.text("", email = _) &
        "type=submit" #> SHtml.ajaxSubmit("送出", process _)
    }
}
