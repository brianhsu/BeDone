package org.bedone.lib

import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw

trait JSImplicit
{
    protected implicit def jsCmdFromStr(str: String): JsCmd = JsRaw(str)
}


