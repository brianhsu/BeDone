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

class GMailStatus extends JSImplicit
{
    private lazy val currentUser = CurrentUser.get.get
    private lazy val gmailFetcher = GMailFetcher(currentUser)

    def formatMessage(error: Option[Throwable]) = error match {
        case None => 
            "#gmailStatus *"       #> S.?("Connected successfully.") & 
            "#gmailStatus [class]" #> "label label-success"
        case Some(error) =>
            "#gmailStatus *"       #> S.?("Connection failed: %s").format(error.getMessage) &
            "#gmailStatus [class]" #> "label label-important"
    }

    def render = {
        gmailFetcher match {
            case None          => "#gmailStatus *" #> S.?("Not set yet")
            case Some(fetcher) => formatMessage(fetcher.validate)
        }
    }

}
