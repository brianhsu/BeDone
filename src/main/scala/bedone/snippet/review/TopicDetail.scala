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

class TopicDetail(topic: Topic) extends JSImplicit
{
    private var currentNavItem = "inbox"
    private val topicID = topic.idField.is

    def template = currentNavItem match {
        case "inbox"      => "lift:embed?what=stuff/list?topicID=" + topicID
        case "nextAction" => "lift:embed?what=action/list?topicID=" + topicID
        case "delegated"  => "lift:embed?what=delegated/list?topicID=" + topicID
        case "scheduled"  => "lift:embed?what=scheduled/list?topicID=" + topicID
        case "maybe"      => "lift:embed?what=maybe/list?topicID=" + topicID
        case "reference"  => "lift:embed?what=reference/list?topicID=" + topicID
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

        ".topicTitle *" #> topic.title.is &
        ".inbox" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("inbox"))) &
        ".nextAction" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("nextAction"))) &
        ".delegated" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("delegated"))) &
        ".scheduled" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("scheduled"))) &
        ".maybe" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("maybe"))) &
        ".reference" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("reference"))) &
        "#detailContent *" #> <div data-lift={template}></div>
    }
}

