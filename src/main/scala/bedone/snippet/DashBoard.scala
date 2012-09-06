package org.bedone.snippet

import org.bedone.model._

import net.liftweb.util.Helpers._
import net.liftweb.http.S

import scala.xml.NodeSeq

class DashBoard
{
    def render = {
        println("QQQQ")
        S.notice("test")
        "Aaa" #> "QQQ"
    }
}
