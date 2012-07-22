package org.bedone.snippet

import org.bedone.model._

import net.liftweb.util.Helpers._
import net.liftweb.util.FieldError

import net.liftweb.record.field._
import net.liftweb.record.BaseField
import net.liftweb.record.Record

import net.liftweb.http.SHtml
import net.liftweb.http.SHtml.ElemAttr
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.jquery.JqJsCmds.JqSetHtml

import scala.xml.NodeSeq
import scala.xml.Text

abstract class AjaxForm[T <: Record[T]]
{
    protected val record: T
    protected def fields = record.allFields.filter(_.name != "idField")

    private lazy val template = Templates("templates-hidden" :: "ajaxForm" :: Nil)

    def showFieldError(fieldID: String, message: NodeSeq): JsCmd = {
        val messageID = fieldID + "_msg"

        JqSetHtml(messageID, message) &
        JsRaw(""" $('#%s').addClass('error') """.format(fieldID))
    }

    def removeFieldError(fieldID: String, helpMessage: NodeSeq = NodeSeq.Empty): JsCmd = {
        val messageID = fieldID + "_msg"

        JqSetHtml(messageID, helpMessage) &
        JsRaw(""" $('#%s').removeClass('error') """.format(fieldID))
    }

    def showFieldError(fieldError: FieldError): JsCmd = {
        showFieldError(fieldError.field.uniqueFieldId.get, fieldError.msg)
    }

    def removeFieldError(field: BaseField): JsCmd = {
        removeFieldError(field.uniqueFieldId.get, field.helpAsHtml.openOr(Text("")))
    }

    def doNothing(value: String) {}

    def ajaxTextField(field: BaseField, defaultValue: String, attrs: ElemAttr*)
                     (setValue: (String) => Any) =
    {
        val fieldID = field.uniqueFieldId.get
        val messageID = fieldID + "_msg"

        def ajaxTest(value: String) = {
            setValue(value)
            validationJS(field, field.validate)
        }

        ".control-group [id]" #> fieldID &
        ".control-group *" #> (
            ".control-label *" #> field.displayName &
            ".help-inline [id]" #> messageID &
            ".help-inline *" #> field.helpAsHtml.openOr(Text("")) &
            "input" #> SHtml.textAjaxTest(defaultValue, doNothing _, ajaxTest _, attrs:_*)
        )
    }

    def validationJS(field: BaseField, validate: List[FieldError]) = validate match {
        case Nil => removeFieldError(field)
        case xs  => validate.map(showFieldError).reduce(_ & _)
    }

    def optionalStringFieldToForm(field: OptionalStringField[_]) =
    {
        val defaultValue = field.defaultValueBox.openOr("").toString

        ajaxTextField(field, defaultValue) { value =>
            field.set(if(value.trim.length > 0) Some(value) else None)
        }
    }

    def stringFieldToForm(field: StringField[_]) =
    {
        ajaxTextField(field, field.defaultValue) { value =>
            field(value)
        }
    }

    def passwordFieldToForm(field: PasswordField[_]) =
    {
        ajaxTextField(field, "", "type" -> "password") { value =>
            field(value)
        } 
    }

    def toForm(field: net.liftweb.record.Field[_, _]) = field match {
        case f: EmailField[_]  => stringFieldToForm(f)
        case f: StringField[_] => stringFieldToForm(f)
        case f: OptionalStringField[_] => optionalStringFieldToForm(f)
        case f: PasswordField[_] => passwordFieldToForm(f)
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

    def cssBinding = fields.map(toForm)

    def toForm: NodeSeq = {
        val cssBinder = "fieldset *" #> cssBinding
        template.map(cssBinder).openOr(<span>Form Generate Error</span>)
    }

}
