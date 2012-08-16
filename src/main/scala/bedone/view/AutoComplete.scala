package org.bedone.view

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.js.JE._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import org.bedone.model._

object AutoComplete {

    def topics = {

        for (term <- S.param("term"); user <- CurrentUser.is) yield {

            val topics = Topic.findByUser(user).openOr(Nil)
            val jsonTopics = topics.filter(_.title.is.contains(term)).map { topic => 
                ("id" -> topic.title.is) ~ ("label" -> topic.title.is)
            }

            JsonResponse(jsonTopics)
        }
    }

    def projects = {

        for (term <- S.param("term"); user <- CurrentUser.is) yield {

            val projects = Project.findByUser(user).openOr(Nil)
            val jsonProjects = projects.filter(_.title.is.contains(term)).map { project => 
                ("id" -> project.title.is) ~ ("label" -> project.title.is)
            }

            JsonResponse(jsonProjects)
        }
    }

    def contexts = {

        for (term <- S.param("term"); user <- CurrentUser.is) yield {

            val contexts = Context.findByUser(user).openOr(Nil)
            val jsonContexts = contexts.filter(_.title.is.contains(term)).map { context => 
                ("id" -> context.title.is) ~ ("label" -> context.title.is)
            }

            JsonResponse(jsonContexts)
        }
    }

    def contacts = {

        for (term <- S.param("term"); user <- CurrentUser.is) yield {

            val contacts = Contact.findByUser(user).openOr(Nil)
            val jsonContacts = contacts.filter(_.name.is.contains(term)).map { contact => 
                ("id" -> contact.name.is) ~ ("label" -> contact.name.is)
            }

            JsonResponse(jsonContacts)
        }
    }

    lazy val autoComplete: LiftRules.DispatchPF = {
        case Req("autocomplete" :: "topic" :: Nil, suffix, GetRequest) => topics _
        case Req("autocomplete" :: "project" :: Nil, suffix, GetRequest) => projects _
        case Req("autocomplete" :: "context" :: Nil, suffix, GetRequest) => contexts _
        case Req("autocomplete" :: "contact" :: Nil, suffix, GetRequest) => contacts _
    }
}
