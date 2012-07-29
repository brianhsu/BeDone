package org.bedone.lib

import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw

trait JSImplicit
{
    object ClearValue
    {
        def apply(id: String) = """$('#%s').val('')""".format(id)
    }

    protected implicit def jsCmdFromStr(str: String): JsCmd = JsRaw(str)
}


