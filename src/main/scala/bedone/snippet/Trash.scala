package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common.Box

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import java.text.SimpleDateFormat

class Trash extends  JSImplicit
{

    private val currentUser = CurrentUser.get.get
    private def trashs = Trash.findByUser(currentUser).openOr(Nil)

    def formatDeadline(stuff: Stuff) = 
    {
        stuff.deadline.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateFormatter.format(calendar.getTime)
        }
    }

    def actionBar(stuff: Stuff) = {

        def starClass = stuff.isStared.is match {
            case true  => "myicon-starOn"
            case false => "myicon-starOff"
        }

        def toogleStar(): JsCmd = {
            stuff.isStared(!stuff.isStared.is)
            stuff.saveTheRecord()
            
            """$('#inboxRow%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def undelete(): JsCmd = {
            println("undelete " + stuff.idField.is)
            stuff.isTrash(false)
            stuff.saveTheRecord()
            new FadeOut("trashRow" + stuff.idField, 0, 500)
        }

        def delete(): JsCmd = {
            println("delete " + stuff.idField.is)
            Stuff.delete(stuff)
            new FadeOut("trashRow" + stuff.idField, 0, 500)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".undelete [onclick]" #> SHtml.onEvent(s => undelete) &
        ".remove [onclick]" #> SHtml.onEvent(s => delete) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#inboxDesc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility
    }

    def createTrashRow(stuff: Stuff) = 
    {
        import TagButton.Implicit._

        def template = Templates("templates-hidden" :: "trash" :: "item" :: Nil)

        val cssBinding = 
            actionBar(stuff) &
            ".trash [id]"    #> ("trashRow" + stuff.idField) &
            ".collapse [id]" #> ("trashDesc" + stuff.idField) &
            ".title *"       #> stuff.titleWithLink &
            ".desc *"        #> stuff.descriptionHTML &
            ".deadline"      #> formatDeadline(stuff)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }
   
    def render = {
        ".trashRow" #> trashs.flatMap(createTrashRow)
    }
}
