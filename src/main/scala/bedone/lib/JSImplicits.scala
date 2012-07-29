package org.bedone.lib

import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw

trait JSImplicit
{
    object ClearValue
    {
        def apply(id: String): JsCmd = """$('#%s').val('')""".format(id)
    }

    object FadeOutAndRemove
    {
        def apply(id: String, timespan: Int = 500): JsCmd = 
            """
                $('#%s').fadeOut(%d, function() { 
                    $('#%s').remove() 
                })
            """.format(id, timespan, id)
    }

    protected implicit def jsCmdFromStr(str: String): JsCmd = JsRaw(str)
}


