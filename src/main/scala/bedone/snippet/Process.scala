package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd

import TagButton.Implicit._


class Process extends JSImplicit
{
    private lazy val currentUser = CurrentUser.is.get
    private lazy val stuff = Stuff.findByUser(currentUser).openOr(Nil)
                                  .filterNot(_.isTrash.is)headOption

    private var currentProjects = stuff.map(_.projects).getOrElse(Nil)
    private var currentTopics = stuff.map(_.topics).getOrElse(Nil)

    def onTopicClick(buttonID: String, topic: Topic) = Noop
    def onProjectClick(buttonID: String, project: Project) = Noop

    def onProjectRemove(buttonID: String, project: Project): JsCmd = {
        currentProjects = currentProjects.filterNot(_ == project)

        FadeOutAndRemove.byClassName("project" + project.idField.is)
    }

    def onTopicRemove(buttonID: String, topic: Topic): JsCmd = {
        currentTopics = currentTopics.filterNot(_ == topic)

        FadeOutAndRemove.byClassName("topic" + topic.idField.is)
    }

    def saveReference(stuff: Stuff, valueAttr: String) = 
    {
        stuff.setTopics(currentTopics)
        stuff.setProjects(currentProjects)
        stuff.stuffType(StuffType.Reference)
        stuff.saveTheRecord()

        S.redirectTo("/process", () => S.notice("已將「%s」加入參考資料" format(stuff.title.is)))
    }

    def markAsTrash(stuff: Stuff, valueAttr: String) = 
    {
        stuff.isTrash(true).saveTheRecord()
        S.redirectTo("/process", () => S.notice("已刪除「%s」" format(stuff.title.is)))
    }
    
    def hasStuffBinding(stuff: Stuff) = 
    {
        "#noStuffAlert" #> "" &
        "name=nextActionTitle [value]" #> stuff.title.is &
        ".projectTags *" #> currentProjects.map(_.editButton(onProjectClick, onProjectRemove)) &
        ".topicTags *" #> currentTopics.map(_.editButton(onTopicClick, onTopicRemove)) &
        "#isTrash [onclick]" #> SHtml.onEvent(markAsTrash(stuff, _)) &
        "#saveReference [onclick]" #> SHtml.onEvent(saveReference(stuff, _))
    }

    def noStuffBinding = 
    {
        ".dialog" #> ""
    }

    def render = {
        stuff match {
            case Some(stuff) => hasStuffBinding(stuff)
            case None        => noStuffBinding
        }
    }
}
