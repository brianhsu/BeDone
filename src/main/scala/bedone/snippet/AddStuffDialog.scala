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
import net.liftweb.record.Record

trait ModalDialog {
    protected val modalID: String
    protected val buttonID: String

    def hideModal: JsCmd = JsRaw("""$('#%s').modal('hide')""".format(modalID))
    def resetButton: JsCmd = JsRaw("""$('#%s').button('reset')""".format(buttonID))
}

class AddStuffDialog extends AjaxForm[Stuff] with ModalDialog
{
    override protected val modalID = "addStuffModal"
    override protected val buttonID = "addStuffButton"
    override protected val formID = Some("addStuffForm")
    override protected val record = Stuff.createRecord
    override protected val fields = List(record.title, record.description, record.deadline)

    def saveAndClose(): JsCmd = {
        record.saveTheRecord() 
        S.redirectTo("/inbox")
        Noop
    }

    def addStuff(): JsCmd =
    {
        record.userID.setBox(CurrentUser.get.map(_.idField.is))
        record.validate match {
            case Nil    => saveAndClose()
            case errors => errors.map(showFieldError) & resetButton
        }
    }

    def render = {
        ".modal-body *" #> this.toForm &
        ".close" #> SHtml.ajaxButton("×", reInitForm _) &
        ".close-link" #> SHtml.a(reInitForm _, Text("取消"), "href" -> "javascript:void(0)") &
        "#addStuffButton" #> SHtml.ajaxButton(Text("新增"), addStuff _)
    }
}

