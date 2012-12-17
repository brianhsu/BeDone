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

class ResetPassword extends JSImplicit
{
    private var newPassword: String = _
    private var confirmPassword: String = _

    val userBox = {
        for (
            username <- S.param("username") ~> S.?("Username is required.");
            code <- S.param("code") ~> S.?("Verification code is required.:");
            user <- User.findByUsername(username) if user.activationCode.is == Some(code) 
        ) yield user
    }

    def process(): JsCmd = {
        if (newPassword.length < 7) {
            S.error(S.?("Password need at least 7 characters"))
        } else if (newPassword != confirmPassword) {
            S.error(S.?("Two passwords are not identical"))
        } else {

            userBox.foreach { user =>
                user.password(newPassword)
                user.activate()
                user.saveTheRecord()
            }

            S.notice(S.?("Your password is changed successfully, please login using your new password."))
            FadeOutAndRemove("resetForm")
        }
    }

    def cssBinding = {
        "#newPassword" #> SHtml.password("", newPassword = _) &
        "#confirmPassword" #> SHtml.password("", confirmPassword = _) &
        "type=submit" #> SHtml.ajaxSubmit(S.?("Change Password"), process _)
    }

    def render = userBox match {
        case Full(user) => cssBinding
        case _ => 
            S.redirectTo(
                "/forgetPassword",
                () => S.error(S.?("Verification code is incorrect, please check your URL again."))
            )
    }

}
