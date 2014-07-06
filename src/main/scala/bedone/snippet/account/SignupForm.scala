package org.bedone.snippet

import org.bedone.model._

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.util.Props

import scala.xml.NodeSeq
import scala.xml.Text

import java.util.Calendar

class SignupDialog extends AjaxForm[User]
{
    private var confirmPassword: String = _

    override protected val record = User.createRecord
    override protected val fields = List(record.username, record.email, record.password)

    def saveAndClose(): JsCmd = {

        val shouldSendActivationCode = Props.getBool("mail.sendActivationMail").getOrElse(true)

        if (shouldSendActivationCode) {
          record.resetActivationCode(ActivationStatus.Register)
          S.notice("已將驗證碼寄至您的信箱，請使用內含的網址啟用您的帳號。以防萬一，也別忘了檢查垃圾郵件資料夾喲！")
        } else {
          record.activate()
          S.notice("註冊完成，您可以使用您的帳號密碼登入 BeDone 了")
        }

        record.saveTheRecord() 

        """$('#signupModal').modal('hide')""" & resetButton
    }

    def resetButton: JsCmd = """$('#signupButton').button('reset')"""

    def signup(): JsCmd = {

        record.validate match {
            case Nil if isPasswordConfirmed => saveAndClose()
            case errors => passwordConfirmJS & this.showAllError(errors) & resetButton
        }
    }

    def isPasswordConfirmed = record.password.match_?(this.confirmPassword)

    def passwordConfirmJS: JsCmd = {
        val fieldID = "confirmPassword"

         isPasswordConfirmed match {
            case true  => removeFieldError(fieldID)
            case false => showFieldError(fieldID, Text(S.?("Two passwords are not identical")))
        }
    }

    def confirmPasswordBinding() =
    {
        val fieldID = "confirmPassword"
        val messageID = fieldID + "_msg"

        def ajaxTest(value: String) = {
            this.confirmPassword = value
            passwordConfirmJS
        }

        ".control-group [id]" #> fieldID &
        ".control-group *" #> (
            ".control-label *" #> S.?("Confirm Password") &
            ".help-inline [id]" #> messageID &
            "input" #> SHtml.textAjaxTest("", doNothing _, ajaxTest _, "type" -> "password")
        )
    }

    override def cssBinding = super.cssBinding :+ confirmPasswordBinding
    override def reInitForm(): JsCmd = super.reInitForm & removeFieldError("confirmPassword")

    def render = {
        ".modal-body *" #> this.toForm &
        ".close" #> SHtml.ajaxButton("×", reInitForm _) &
        ".close-link" #> SHtml.a(reInitForm _, Text(S.?("Cancel")), "href" -> "javascript:void(0)") &
        "#signupButton" #> SHtml.ajaxButton(Text(S.?("Sign up")), signup _)
    }
}

