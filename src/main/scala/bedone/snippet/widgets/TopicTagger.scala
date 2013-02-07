package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds._

import TagButton.Implicit._

trait TopicTagger extends JSImplicit
{
    var currentTopics: List[Topic]
    val topicTagContainers: List[String]

    def onTopicClick(buttonID: String, topic: Topic) = Noop

    def onTopicRemove(buttonID: String, topic: Topic): JsCmd = 
    {
        currentTopics = currentTopics.filterNot(_ == topic)
        FadeOutAndRemove.byClassName(topic.className)
    }

    def appendTopicTagJS(topic: Topic) =
    {
        topicTagContainers.map { htmlID => 
            AppendHtml(htmlID, topic.editButton(onTopicClick, onTopicRemove))
        }
    }

    def createTopicTags(containerID: String) =
    {
        val topicCombobox = new TopicComboBox{

            def addTopic(topic: Topic) = {
                currentTopics.map(_.title.is).contains(topic.title.is) match {
                    case true  => this.clear
                    case false =>
                        currentTopics ::= topic
                        this.clear & appendTopicTagJS(topic)
                }
            }
        }

        ".topicCombo"     #> topicCombobox.comboBox &
        ".topicTags [id]" #> containerID &
        ".topicTags" #> (
            "span" #> currentTopics.map(_.editButton(onTopicClick, onTopicRemove))
        )
    }
}

