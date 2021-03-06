package org.bedone.lib

import org.bedone.model._

import net.liftweb.common.Box
import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.Templates

import scala.xml.NodeSeq

object TagButton
{
    def topicView   = Templates("templates-hidden" :: "button" :: "topic" :: Nil)
    def projectView = Templates("templates-hidden" :: "button" :: "project" :: Nil)
    def contextView = Templates("templates-hidden" :: "button" :: "context" :: Nil)
    def contactView = Templates("templates-hidden" :: "button" :: "contact" :: Nil)

    object Implicit {
        implicit def fromTopic(topic: Topic) = 
            new TagViewButton(topicView, topic, topic.title.is, topic.className)

        implicit def fromProject(project: Project) = 
            new TagViewButton(projectView, project, project.title.is, project.className)

        implicit def fromContext(context: Context) = 
            new TagViewButton(contextView, context, context.title.is, context.className)

        implicit def fromContact(contact: Contact) = 
            new TagViewButton(contactView, contact, contact.name.is, contact.className)

    }
}

class TagViewButton[T](template: Box[NodeSeq], data: T, title: String, className: String)
{
    type OnClick = (String, T) => JsCmd
    type OnRemove = (String, T) => JsCmd

    def viewButton(onClick: OnClick) = {
        val buttonID = randomString(20)
        val cssBinding =
            "div [id]" #> buttonID &
            "div [class+]" #> className &
            "div [onclick]" #> SHtml.onEvent(s => onClick(buttonID, data)) &
            ".title *" #> title &
            ".icon-remove" #> ""

        template.map(cssBinding).openOr(<span>Generate Button Error</span>)
    }

    def editButton(onClick: OnClick, onRemove: OnRemove) = {
        val buttonID = randomString(20)
        val cssBinding =
            "div [id]" #> buttonID &
            "div [class+]" #> className &
            "div [onclick]" #> SHtml.onEvent(s => onClick(buttonID, data)) &
            ".title *" #> title &
            ".icon-remove [onclick]" #> SHtml.onEvent(s => onRemove(buttonID, data))

        template.map(cssBinding).openOr(<span>Generate Button Error</span>)
    }


}

