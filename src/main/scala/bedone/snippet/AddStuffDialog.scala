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

class AddStuffDialog extends AjaxForm[Stuff]
{
    private implicit def jsCmdFromStr(str: String): JsCmd = JsRaw(str)

    override protected val record = Stuff.createRecord
    override protected val fields = List(record.title, record.description, record.deadline)


    def render = {
        println(record.deadline.toForm)
        ".modal-body *" #> this.toForm
    }
}

