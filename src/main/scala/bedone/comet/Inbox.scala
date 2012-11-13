package org.bedone.comet

import org.bedone.model._
import org.bedone.lib._
import org.bedone.snippet._

import net.liftweb.actor.LiftActor

import net.liftweb.common.Box

import net.liftweb.util.Helpers._
import net.liftweb.util.Schedule

import net.liftweb.http.S
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.http.CometActor

import scala.xml.NodeSeq

class GMailListener extends LiftActor
{
    def getMails(user: User) = GMailFetcher(user).map(_.sync).getOrElse(Nil)
    
    def messageHandler = {
        case FetchGMail(user, comet) => 
            println("FetchMail:(%s, %s)" format(comet.hashCode, user))
            comet ! NewStuffs(getMails(user))
            Schedule.schedule(this, FetchGMail(user, comet), 5 minutes)
    }
}


class Inbox extends CometActor with StuffList
{
    val projectID: Box[Int] = S.attr("projectID").map(_.toInt)
    val topicID: Box[Int] = S.attr("topicID").map(_.toInt)
    val gmailListener = new GMailListener

    override def render = NodeSeq.Empty

    override def lowPriority = {
        case NewStuffs(mails) => 
            println("new mail of %s: %s".format(this, mails))
            partialUpdate(AppendHtml("inboxList", mails.flatMap(createStuffRow)))
    }

    println("send init mail fetcher request")
    gmailListener ! FetchGMail(currentUser, this)
}
