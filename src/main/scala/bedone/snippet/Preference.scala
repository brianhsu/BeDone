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
    private lazy val oldSetting = GMailPreference.findByUser(currentUser).toOption

    private var gmailAccount: String = oldSetting.map(_.username.is).getOrElse("")
    private var gmailPassword: String = _
    private var usingGMail: Boolean = oldSetting.map(_.usingGMail.is).getOrElse(false)

    def saveGMail() =
    {
        def getGMailPassword = {
            if (gmailPassword.length == 0) {
                oldSetting.flatMap(x => PasswordHelper.decrypt(x.password.is)).getOrElse("")
            } else {
                gmailPassword
            }
        }

        def updateStatusFailed(error: Throwable): JsCmd = 
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

        def updateStatusOK: JsCmd =
        {
            val encryptedPassword = PasswordHelper.encrypt(gmailPassword)
            val preference = GMailPreference.findByUser(currentUser)
                                            .openOr(GMailPreference.createRecord)

            preference.idField(currentUser.idField.is)
                      .username(gmailAccount)
                      .password(encryptedPassword)
                      .usingGMail(usingGMail)
                      .saveTheRecord()

            """$('#gmailStatus').attr('class', 'label label-success')""" &
            """$('#gmailStatus').text('連線成功，已儲存設定')"""
        }

        val fetcher = new GMailFetcher(currentUser.idField.is, gmailAccount, getGMailPassword)

        fetcher.validate match {
            case Some(error) => updateStatusFailed(error)
            case None        => updateStatusOK
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
