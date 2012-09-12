package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import java.text.SimpleDateFormat
import scala.xml.NodeSeq

class ProjectDetail(project: Project) extends JSImplicit
{
    private var currentNavItem = "inbox"
    private val projectID = project.idField.is

    def template = currentNavItem match {
        case "inbox"      => "lift:embed?what=stuff/list?projectID=" + projectID
        case "nextAction" => "lift:embed?what=action/list?projectID=" + projectID
        case "delegated"  => "lift:embed?what=delegated/list?projectID=" + projectID
        case "scheduled"  => "lift:embed?what=scheduled/list?projectID=" + projectID
        case "maybe"      => "lift:embed?what=maybe/list?projectID=" + projectID
        case "reference"  => "lift:embed?what=reference/list?projectID=" + projectID
    }


    def updateContent(): JsCmd =
    {
        val content = <div data-lift={template}></div>

        "$('.navItem').removeClass('active')" &
        "$('.%s').addClass('active')".format(currentNavItem) &
        JqSetHtml("detailContent", content)
    }

    def switchNavItem(item: String) = {

        this.currentNavItem = item

        updateContent()
    }

    def render = {

        ".projectTitle *" #> project.title.is &
        ".inbox" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("inbox"))) &
        ".nextAction" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("nextAction"))) &
        ".delegated" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("delegated"))) &
        ".scheduled" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("scheduled"))) &
        ".maybe" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("maybe"))) &
        ".reference" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("reference"))) &
        "#detailContent *" #> <div data-lift={template}></div>
    }
}

