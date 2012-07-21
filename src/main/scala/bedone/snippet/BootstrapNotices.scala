package org.bedone.snippet

import org.bedone.model._

import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml.ElemAttr
import scala.xml.NodeSeq

class BootstrapNotices
{
    def addCloseButton(html: NodeSeq) = {
        <button class="close" data-dismiss="alert">×</button> ++ html
    }

    def render = {
        ".alert *" #> addCloseButton _
    }
}
