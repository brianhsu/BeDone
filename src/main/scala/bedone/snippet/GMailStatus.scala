package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

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
            "#gmailStatus *" #> "連線成功" & 
            "#gmailStatus [class]" #> "label label-success"
        case Some(error) =>
            "#gmailStatus *" #> "連線失敗，%s".format(error.getMessage) &
            "#gmailStatus [class]" #> "label label-important"
    }

    def render = {
        gmailFetcher match {
            case None          => "#gmailStatus *" #> "尚未設定"
            case Some(fetcher) => formatMessage(fetcher.validate)
        }
    }

}
