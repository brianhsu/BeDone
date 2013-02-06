package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.http.S
import net.liftweb.util.Helpers._


class NextActionModal extends ProjectTagger with TopicTagger 
                      with ContextTagger with JSImplicit
{
    val stuff = S.attr("stuffID")
                 .flatMap(stuffID => Stuff.findByID(stuffID.toInt))
                 .toOption

    override val projectTagContainers = List("nextActionProject")
    override val topicTagContainers = List("nextActionTopic")
    override val contextTagContainers = List("nextActionContext")

    override var currentProjects = stuff.map(_.projects).getOrElse(Nil)
    override var currentTopics = stuff.map(_.topics).getOrElse(Nil)
    override var currentContexts: List[Context] = Nil

    def render = {
        createProjectTags("nextActionProject") &
        createTopicTags("nextActionTopic") &
        createContextTags("nextActionContext")
    }
}
