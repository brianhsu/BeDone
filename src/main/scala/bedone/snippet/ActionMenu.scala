package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.http.SHtml
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._
import net.liftweb.http.S
import scala.xml.NodeSeq

object DragDropHandler extends JSImplicit
{
    def removeRow(stuffID: String): JsCmd = JsRaw(
    """
        $('div[data-stuffid="%s"]').fadeOut(500, function() {
            $('div[data-stuffid="%s"]').remove();
        })
    """.format(stuffID, stuffID)
    )

    val markAsTrash = {

        def markAsTrashInDB (stuffID: String) = {

            Stuff.findByID(stuffID.toInt)
                 .foreach(_.isTrash(true).saveTheRecord)

            removeRow(stuffID) &
            """updatePaging()"""
        }
        
        Function(
            "markAsTrash", List("stuffID"), 
            SHtml.ajaxCall(JsRaw("stuffID"), markAsTrashInDB _)
        )
    }
}

class ActionMenu
{
    def render = {

        val menuID = "#" + S.request.map(_.path(0)).openOr("nonMenu")

        (menuID + " [class]") #> "active" &
        (menuID) #> ("i [class+]" #> "icon-white") &
        "#menuAjaxJS" #> Script(DragDropHandler.markAsTrash)
    }
}
