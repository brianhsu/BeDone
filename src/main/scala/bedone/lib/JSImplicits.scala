package org.bedone.lib

import net.liftweb.util.FieldError
import net.liftweb.util.FieldIdentifier

import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.jquery.JqJsCmds._

import scala.xml.Text

trait JSImplicit
{
    protected implicit def jsCmdFromStr(str: String): JsCmd = JsRaw(str)
    protected implicit def xmlFromStr(x: String) = Text(x)

    object ClearValue
    {
        def apply(id: String): JsCmd = """$('#%s').val('')""".format(id)
    }

    object FadeOutAndRemove
    {
        def byClassName(className: String, timespan: Int = 500): JsCmd = 
        {
            """
                $('.%s').fadeOut(%d, function() {
                    $('.%s').remove()
                })
            """.format(className, timespan, className)
        }

        def apply(id: String, timespan: Int = 500): JsCmd = 
            """
                $('#%s').fadeOut(%d, function() { 
                    $('#%s').remove() 
                })
            """.format(id, timespan, id)
    }

    object ClearFieldError {
        def apply(id: String, message: String = "") = {
            val messageID = id + "_msg"
            """$('#%s').removeClass("error")""".format(id) &
            JqSetHtml(messageID, message)
        }
    }

    object ShowFieldError {
        def apply(id: String, message: String) = {
            val messageID = id + "_msg"
            """$('#%s').addClass("error")""".format(id) &
            JqSetHtml(messageID, message)
        }
    }

    object JqEmpty {
        def apply(uid: String): JsCmd = """$('#isDone').empty()"""
    }

    object JqSetVisible
    {
        def apply(uid: String, display: Boolean) = display match {
            case true  => Show(uid)
            case false => Hide(uid)
        }
    }

    def setError(fieldErrors: List[FieldError], fieldID: String): (Boolean, JsCmd) = {

        fieldErrors match {
            case Nil    => (false, ClearFieldError(fieldID))
            case errors => 
                val message = errors.map(_.msg).mkString(",")
                (true, ShowFieldError(fieldID, message))
        }
    }


}


