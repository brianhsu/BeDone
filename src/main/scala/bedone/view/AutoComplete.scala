package org.bedone.view

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.js.JE._

import org.bedone.model._

object AutoComplete {

    def topics = {

        for (term <- S.param("term"); user <- CurrentUser.is) yield {

            val topics = Topic.findByUser(user).openOr(Nil)
            val jsonTopics = topics.filter(_.title.is.contains(term)).map { topic => 
                """{"id": "%s", "label": "%s"}""" format(topic.title, topic.title)
            }

            JsonResponse(JsRaw("""[%s]""" format(jsonTopics.mkString(","))))
        }
    }

    def projects = {

        for (term <- S.param("term"); user <- CurrentUser.is) yield {

            val projects = Project.findByUser(user).openOr(Nil)
            val jsonProjects = projects.filter(_.title.is.contains(term)).map { project => 
                """{"id": "%s", "label": "%s"}""" format(project.title, project.title)
            }

            JsonResponse(JsRaw("""[%s]""" format(jsonProjects.mkString(","))))
        }
    }

    lazy val autoComplete: LiftRules.DispatchPF = {
        case Req("autocomplete" :: "topic" :: Nil, suffix, GetRequest) => topics _
        case Req("autocomplete" :: "project" :: Nil, suffix, GetRequest) => projects _
    }
}
