package org.bedone.snippet

import org.bedone.model._

import net.liftweb.common.Box
import net.liftweb.util.Helpers._
import net.liftweb.util.FieldError

import net.liftweb.record.field._
import net.liftweb.record.Field
import net.liftweb.record.BaseField
import net.liftweb.record.Record

import net.liftweb.util.FieldIdentifier

import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds.JqSetHtml
import net.liftweb.http.js.JE.JsRaw

import scala.xml.NodeSeq
import scala.xml.Text

class SignupDialog {

    val record = Publisher.createRecord

    object Form extends AjaxForm[Publisher] {
        val record = SignupDialog.this.record
    }

    def closeDialog: JsCmd = JsRaw(""" $('#signupModal').modal('toogle') """)
    def signup(): JsCmd = {

        record.validate match {
            case Nil    => closeDialog
            case errors => 
                println(errors) 
                Form.showAllError(errors)
        }
    }

    def render = 
        ".modal-body *" #> Form.toForm &
        ".btn-primary" #> SHtml.a(signup _, Text("註冊"))
                
}

abstract class AjaxForm[T <: Record[T]]
{
    val record: T

    private lazy val xs = record.allFields.filter(_.name != "idField")
    private lazy val template = Templates("templates-hidden" :: "test" :: Nil)

    def showFieldError(fieldError: FieldError): JsCmd =
    {
        val fieldID = fieldError.field.uniqueFieldId.get
        val messageID = fieldID + "_msg"

        JqSetHtml(messageID, <span>{fieldError.msg}</span>) &
        JsRaw(""" $('#%s').addClass('error') """.format(fieldID))
    }

    def removeFieldError(field: BaseField) =
    {
        val fieldID = field.uniqueFieldId.get
        val messageID = fieldID + "_msg"

        JqSetHtml(messageID, field.helpAsHtml.openOr(Text(""))) &
        JsRaw(""" $('#%s').removeClass('error') """.format(fieldID))
    }

    def ajaxTextField(field: BaseField, defaultValue: String, ajaxTest: (String) => JsCmd) =
    {
        val fieldID = field.uniqueFieldId.get
        val messageID = fieldID + "_msg"

        def doNothing(value: String) {}

        ".control-group [id]" #> fieldID &
        ".control-group *" #> (
            ".control-label *" #> field.displayName &
            ".help-inline [id]" #> messageID &
            ".help-inline *" #> field.helpAsHtml.openOr(Text("")) &
            "input" #> SHtml.textAjaxTest(defaultValue, doNothing _, ajaxTest)
        )
    }

    def validationJS(field: BaseField, validate: List[FieldError]) = validate match {
        case Nil => removeFieldError(field)
        case xs  => validate.map(showFieldError).reduce(_ & _)
    }

    def optionalStringFieldToForm(field: OptionalStringField[_]) =
    {
        def ajaxTest(value: String) = {
            field.set(if(value.trim.length > 0) Some(value) else None)
            validationJS(field, field.validate)
        }

        ajaxTextField(field, field.defaultValueBox.openOr("").toString, ajaxTest _)
    }

    def stringFieldToForm(field: StringField[_], checkEmpty: Boolean = true) =
    {
        def ajaxTest(value: String) = {
            field(value)
            validationJS(field, field.validate)
        }

        ajaxTextField(field, field.defaultValue, ajaxTest _)
    }

    def toForm(field: net.liftweb.record.Field[_, _]) = field match {
        case f: EmailField[_]  => stringFieldToForm(f, false)
        case f: StringField[_] => stringFieldToForm(f)
        case f: OptionalStringField[_] => optionalStringFieldToForm(f)
        case y => ".control-label *" #> ("Not String Type " + y.name)
    }

    def showAllError(error: List[FieldError]): JsCmd = {
        
        val errorFieldIDs = error.map(_.field.uniqueFieldId.get)
        val okFields = record.allFields.filterNot { field => 
            errorFieldIDs.contains(field.uniqueFieldId.get)
        }

        okFields.map(removeFieldError) ++
        error.map(showFieldError)
    }

    def toForm: NodeSeq = {
        val cssBinder = "fieldset *" #> record.allFields.filter(_.name != "idField").map(toForm)
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }

}
