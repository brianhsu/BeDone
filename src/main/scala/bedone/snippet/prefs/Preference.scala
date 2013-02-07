package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.S
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
            val errorMessage = S.?("Connection failed: %s").format(error.getMessage)

            """$('#gmailStatus').attr('class', 'label label-important')""" &
            "$('#gmailStatus').text('" + errorMessage + "')"
        }

        def updateStatusOK: JsCmd =
        {
            val encryptedPassword = PasswordHelper.encrypt(getGMailPassword)
            val preference = GMailPreference.findByUser(currentUser)
                                            .openOr(GMailPreference.createRecord)

            preference.idField(currentUser.idField.is)
                      .username(gmailAccount)
                      .password(encryptedPassword)
                      .usingGMail(usingGMail)
                      .saveTheRecord()

            val successMessage = S.?("Connected successfully, setting is saved.")

            """$('#gmailStatus').attr('class', 'label label-success')""" &
            "$('#gmailStatus').text('" + successMessage + "')"
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
        "#saveGMail" #> SHtml.ajaxSubmit(S.?("Save Setting"), saveGMail _)
    }
}
