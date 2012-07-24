package org.bedone.view

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.js.JE._

import org.bedone.model._

object AutoComplete {

    def xmlResponse = {

        for (term <- S.param("term"); user <- CurrentUser.is) yield {

            val topics = StuffTopic.findByUser(user).map{ xs => 
                xs.map(_.topic.is).filter(_.contains(term)).distinct
            }

            val jsonTopics = topics.openOr(Nil).map { topic => 
                """{"id": "%s", "label": "%s"}""" format(topic, topic)
            }

            JsonResponse(JsRaw("""[%s]""" format(jsonTopics.mkString(","))))
        }


    }

    lazy val autoComplete: LiftRules.DispatchPF = {
        case Req("autocomplete" :: "topic" :: Nil, suffix, GetRequest) => xmlResponse _
    }
}
