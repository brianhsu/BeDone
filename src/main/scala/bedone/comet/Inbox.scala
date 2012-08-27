package org.bedone.comet

import org.bedone.model._
import org.bedone.lib._
import org.bedone.snippet._

import net.liftweb.actor.LiftActor

import net.liftweb.util.Helpers._
import net.liftweb.util.Schedule

import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.http.CometActor

import scala.xml.NodeSeq

case class FetchGMail(user: User, sender: CometActor)
case class NewStuffs(mails: List[Stuff])

object GMailListener extends LiftActor
{
    def getMails(user: User) = GMailFetcher(user).map(_.sync).getOrElse(Nil)

    def messageHandler = {
        case FetchGMail(user, sender) => 
            println("Get FetchMail request")
            sender ! NewStuffs(getMails(user))
            Schedule.schedule(this, FetchGMail(user, sender), 5 minutes)
    }
}

class Inbox extends CometActor with StuffList
{
    override def render = NodeSeq.Empty

    override def lowPriority = {
        case NewStuffs(mails) => 
            println("get new mails:" + mails)
            partialUpdate(AppendHtml("inboxList", mails.flatMap(createStuffRow)))
    }

    GMailListener ! FetchGMail(currentUser, this)
}
