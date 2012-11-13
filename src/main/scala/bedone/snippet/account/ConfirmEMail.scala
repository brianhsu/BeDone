package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common._

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
    val user = {
        for (
            username <- S.param("username") ~> "無使用者名稱";
            code <- S.param("code") ~> "無驗證碼";
            user <- User.findByUsername(username) if user.activationCode.is == Some(code) 
        ) yield user
    }

    def render = {

        user match {
            case Full(user) => {
                user.activate()
                println(user.saveTheRecord())
                S.notice("已啟用您的帳號，您可以使用您的帳號登入 BeDone 囉！")
            }
            case Empty => S.error("驗證碼錯誤，請再次檢查您的驗證網址")
            case Failure(_, _, msg) => S.error("無法驗證此帳號：" + msg)
        }

        PassThru
    }
}
