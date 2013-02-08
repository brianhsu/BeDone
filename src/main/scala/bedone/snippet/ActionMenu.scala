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


class DragDropHandler extends JSImplicit
{

    val showNextActionDialog = {

        def showModal (stuffID: String) = {
            println("Show Next Action Modal:" + stuffID)
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
            println("Show Delegated Modal:" + stuffID)

            val snippet = "lift:embed?what=modal/DelegatedModal;stuffID=" + stuffID
            JqSetHtml("modalBox", <div data-lift={snippet} />)
        }
        
        Function(
            "showDelegatedDialog", List("stuffID"), 
            SHtml.ajaxCall(JsRaw("stuffID"), showModal _)
        )
    }

    val showMaybeDialog = {

        def showModal (stuffID: String) = {
            println("Show Maybe Modal:" + stuffID)

            val snippet = "lift:embed?what=modal/MaybeModal;stuffID=" + stuffID
            JqSetHtml("modalBox", <div data-lift={snippet} />)
        }
        
        Function(
            "showMaybeDialog", List("stuffID"), 
            SHtml.ajaxCall(JsRaw("stuffID"), showModal _)
        )
    }

    val showReferenceDialog = {

        def showModal (stuffID: String) = {
            println("Show Reference Modal:" + stuffID)

            val snippet = "lift:embed?what=modal/ReferenceModal;stuffID=" + stuffID
            JqSetHtml("modalBox", <div data-lift={snippet} />)
        }
        
        Function(
            "showReferenceDialog", List("stuffID"), 
            SHtml.ajaxCall(JsRaw("stuffID"), showModal _)
        )
    }

    val showScheduledDialog = {

        def showModal (stuffID: String) = {
            println("Show Scheduled Modal:" + stuffID)

            val snippet = "lift:embed?what=modal/ScheduledModal;stuffID=" + stuffID
            JqSetHtml("modalBox", <div data-lift={snippet} />)
        }
        
        Function(
            "showScheduledDialog", List("stuffID"), 
            SHtml.ajaxCall(JsRaw("stuffID"), showModal _)
        )
    }

    val markAsTrash = {

        def markAsTrashInDB (stuffID: String) = {

            println("Mark as trash:" + stuffID)

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
    def menuIDBox = S.request.map { url =>
        val lastURLIndex = (0 max (url.path.partPath.size - 1))
        url.path(lastURLIndex)
    }

    def render = {

        val menuID = "#" + menuIDBox.openOr("nonMenu")
        val dragDropHandler = new DragDropHandler

        (menuID + " [class]") #> "active" &
        (menuID) #> ("i [class+]" #> "icon-white") &
        "#menuAjaxJS" #> Script(
            dragDropHandler.markAsTrash & 
            dragDropHandler.showNextActionDialog &
            dragDropHandler.showReferenceDialog &
            dragDropHandler.showDelegatedDialog &
            dragDropHandler.showMaybeDialog &
            dragDropHandler.showScheduledDialog
        )
    }
}
