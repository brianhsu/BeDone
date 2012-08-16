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

    def contexts = {

        for (term <- S.param("term"); user <- CurrentUser.is) yield {

            val contexts = Context.findByUser(user).openOr(Nil)
            val jsonContexts = contexts.filter(_.title.is.contains(term)).map { context => 
                """{"id": "%s", "label": "%s"}""" format(context.title, context.title)
            }

            JsonResponse(JsRaw("""[%s]""" format(jsonContexts.mkString(","))))
        }
    }

    def contacts = {

        for (term <- S.param("term"); user <- CurrentUser.is) yield {

            val contacts = Contact.findByUser(user).openOr(Nil)
            val jsonContacts = contacts.filter(_.name.is.contains(term)).map { contact => 
                """{"id": "%s", "label": "%s"}""" format(contact.name, contact.name)
            }

            JsonResponse(JsRaw("""[%s]""" format(jsonContacts.mkString(","))))
        }
    }

    lazy val autoComplete: LiftRules.DispatchPF = {
        case Req("autocomplete" :: "topic" :: Nil, suffix, GetRequest) => topics _
        case Req("autocomplete" :: "project" :: Nil, suffix, GetRequest) => projects _
        case Req("autocomplete" :: "context" :: Nil, suffix, GetRequest) => contexts _
        case Req("autocomplete" :: "contact" :: Nil, suffix, GetRequest) => contacts _
    }
}
