package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import java.text.SimpleDateFormat

class Preference extends JSImplicit
{
    private lazy val currentUser = CurrentUser.get.get

    private var gmailAccount: String = _
    private var gmailPassword: String = _
    private var usingGMail: Boolean = false


    def saveGMail() =
    {
        println("GMailAccount:" + gmailAccount)
        println("GMailPassword:" + gmailPassword)
        println("gmailCheckbox:" + usingGMail)

        def updateStatusFailedJS(error: Throwable): JsCmd = 
        {
            val errorMessage = error.getMessage
            val reason = if (errorMessage.contains("Invalid credentials")) {
                "帳號密碼有誤，請檢查帳號密碼後重新設定"
            } else if (errorMessage.contains("Your account is not enabled for IMAP use.")) {
                "尚未在 GMail 中開啟 IMAP 選項"
            } else {
                errorMessage
            }

            """$('#gmailStatus').attr('class', 'label label-important')""" &
            """$('#gmailStatus').text('連線失敗，%s')""".format(reason)
        }

        def updateStatusOKJS: JsCmd =
        {
            println("OK")
            """$('#gmailStatus').attr('class', 'label label-success')""" &
            """$('#gmailStatus').text('連線成功，已儲存設定')"""
        }

        val fetcher = new GMailFetcher(currentUser.idField.is, gmailAccount, gmailPassword)

        fetcher.validate match {
            case Some(error) => updateStatusFailedJS(error)
            case None        => updateStatusOKJS
        }

    }

    def render = {
        "#avatar [src]" #> currentUser.avatarURL &
        "#gmailAccount" #> SHtml.text(gmailAccount, gmailAccount = _) &
        "#gmailPassword" #> SHtml.text(gmailPassword, gmailPassword = _, "type" -> "password") &
        "#usingGMail" #> SHtml.checkbox(usingGMail, usingGMail = _) &
        "#saveGMail" #> SHtml.ajaxSubmit("儲存設定", saveGMail _)
    }
}
