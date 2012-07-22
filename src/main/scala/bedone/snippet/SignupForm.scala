package org.bedone.snippet

import org.bedone.model._

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw

import scala.xml.NodeSeq
import scala.xml.Text

class SignupDialog extends AjaxForm[User]
{
    private var confirmPassword: String = _

    override protected val record = User.createRecord
    override protected val fields = List(record.username, record.email, record.password)

    def saveAndClose(): JsCmd = {
        record.saveTheRecord() 
        JsRaw(""" $('#signupModal').modal('hide') """)
    }

    def signup(): JsCmd = {

        record.validate match {
            case Nil if isPasswordConfirmed => saveAndClose()
            case errors => passwordConfirmJS & this.showAllError(errors)
        }
    }

    def isPasswordConfirmed = record.password.match_?(this.confirmPassword)

    def passwordConfirmJS: JsCmd = {
        val fieldID = "confirmPassword"

         isPasswordConfirmed match {
            case true  => removeFieldError(fieldID)
            case false => showFieldError(fieldID, Text("密碼不一致"))
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
            ".control-label *" #> "Confirm Password" &
            ".help-inline [id]" #> messageID &
            "input" #> SHtml.textAjaxTest("", doNothing _, ajaxTest _, "type" -> "password")
        )
    }

    override def cssBinding: List[net.liftweb.util.CssSel] = 
        super.cssBinding :+ confirmPasswordBinding

    def reInitForm(): JsCmd = 
        fields.map(removeFieldError) &
        removeFieldError("confirmPassword")

    def render = 
        ".modal-body *" #> this.toForm &
        ".close" #> SHtml.ajaxButton("×", reInitForm _) &
        ".close-link" #> SHtml.a(reInitForm _, Text("取消"), "href" -> "javascript:void(0)") &
        ".btn-primary" #> SHtml.a(signup _, Text("註冊"), "href" -> "javascript:void(0)")
}

