package org.bedone.snippet

import net.liftweb.util.Helpers._
import net.liftweb.http.S

import org.scribe.builder._
import org.scribe.model._
import org.scribe.builder.api._

import scala.xml.XML


class ContactOAuth
{
    def render = {

        val data = ContactsOAuth.is.map { api =>
            api.setAccessToken(S.param("oauth_verifier").openOr(""))
            api.contacts
        }


        println(data.map(_.mkString("\n")))
        "aaa" #> "QQQ"
    }
}
