package org.bedone.snippet

import net.liftweb.util.Helpers._
import net.liftweb.http.S
import scala.xml.NodeSeq

class ActionMenu
{
    
    def render = {

        val menuID = "#" + S.request.map(_.path(0)).openOr("nonMenu")

        (menuID + " [class]") #> "active" &
        (menuID) #> ("i [class+]" #> "icon-white")
    }
}
