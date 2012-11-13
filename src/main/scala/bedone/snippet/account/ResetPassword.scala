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
            username <- S.param("username") ~> "無使用者名稱";
            code <- S.param("code") ~> "無驗證碼";
            user <- User.findByUsername(username) if user.activationCode.is == Some(code) 
        ) yield user
    }

    def process(): JsCmd = {
        if (newPassword.length < 7) {
            S.error("密碼至少要七個字元")
        } else if (newPassword != confirmPassword) {
            S.error("新密碼和確認密碼不符")
        } else {

            userBox.foreach { user =>
                user.password(newPassword)
                user.activate()
                user.saveTheRecord()
            }

            S.notice("已更改密碼，請使用新密碼登入")
            FadeOutAndRemove("resetForm")
        }
    }

    def cssBinding = {
        "#newPassword" #> SHtml.password("", newPassword = _) &
        "#confirmPassword" #> SHtml.password("", confirmPassword = _) &
        "type=submit" #> SHtml.ajaxSubmit("更改密碼", process _)
    }

    def render = userBox match {
        case Full(user) => cssBinding
        case _ => 
            S.redirectTo(
                "/forgetPassword",
                () => S.error("驗證碼錯誤，請再次檢查您的驗證網址")
            )
    }

}
