package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.http.SHtml
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.util.Helpers._
import net.liftweb.http.S
import scala.xml.NodeSeq


object DragDropHandler extends JSImplicit
{

    val showNextActionDialog = {

        def showModal (stuffID: String) = {
            val snippet = "lift:embed?what=modal/NextActionModal;stuffID=" + stuffID
            JqSetHtml("modalBox", <div data-lift={snippet} />)
        }
        
        Function(
            "showNextActionDialog", List("stuffID"), 
            SHtml.ajaxCall(JsRaw("stuffID"), showModal _)
        )
    }

    val showDelegatedDialog = {

        def showModal (stuffID: String) = {
            val snippet = "lift:embed?what=modal/DelegatedModal;stuffID=" + stuffID
            JqSetHtml("modalBox", <div data-lift={snippet} />)
        }
        
        Function(
            "showDelegatedDialog", List("stuffID"), 
            SHtml.ajaxCall(JsRaw("stuffID"), showModal _)
        )
    }

    val showReferenceDialog = {

        def showModal (stuffID: String) = {
            val snippet = "lift:embed?what=modal/ReferenceModal;stuffID=" + stuffID
            JqSetHtml("modalBox", <div data-lift={snippet} />)
        }
        
        Function(
            "showReferenceDialog", List("stuffID"), 
            SHtml.ajaxCall(JsRaw("stuffID"), showModal _)
        )
    }

    val markAsTrash = {

        def markAsTrashInDB (stuffID: String) = {

            Stuff.findByID(stuffID.toInt)
                 .foreach(_.isTrash(true).saveTheRecord)

            RemoveInboxRow(stuffID) &
            """updatePaging()""" &
            """addDraggable()"""
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
        "#menuAjaxJS" #> Script(
            DragDropHandler.markAsTrash & 
            DragDropHandler.showNextActionDialog &
            DragDropHandler.showReferenceDialog &
            DragDropHandler.showDelegatedDialog
        )
    }
}
