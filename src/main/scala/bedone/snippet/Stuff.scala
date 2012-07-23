package org.bedone.snippet

import org.bedone.model._

import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml.ElemAttr
import scala.xml.NodeSeq

class Inbox
{
    lazy val stuffs = CurrentUser.get.flatMap(Stuff.findByUser _).openOr(Nil)

    def render = {
        "aaa" #> "QQQ"
    }
}
