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

case class NewMail(username: String, stuffs: List[Stuff])
case class FetchGMail(user: User)
case class RegisterInboxComet(inboxComet: InboxComet)
case class UnregisterInboxComet(inboxComet: InboxComet)

class GMailListener extends LiftActor
{
    def getMails(user: User) = GMailFetcher(user).map(_.sync).getOrElse(Nil)
    
    def messageHandler = {
        case FetchGMail(user) => MailCenter ! NewMail(user.username.is, getMails(user))
    }
}

object MailCenter extends LiftActor
{
    var cometSet: Set[InboxComet] = Set()
    var mailFetcher: Map[String, GMailListener] = Map()

    def addComet(comet: InboxComet)
    {
        cometSet += comet

        if (!mailFetcher.contains(comet.username)) {
            mailFetcher += (comet.username -> new GMailListener)
        }
    }

    def removeComet(comet: InboxComet)
    {
        val currentUsername = comet.username

        cometSet -= comet

        if (!cometSet.map(_.username).contains(currentUsername)) {
            mailFetcher -= currentUsername
        }
    }

    def transferRequest(request: FetchGMail)
    {
        val username = request.user.username.is
        mailFetcher.get(username).foreach(_ ! request)
    }

    def broadcastToComet(newMail: NewMail) 
    {
        cometSet.filter(_.username == newMail.username).foreach(_ ! newMail)
    }

    def messageHandler = 
    {
        case RegisterInboxComet(inboxComet) => addComet(inboxComet)
        case UnregisterInboxComet(inboxComet) => removeComet(inboxComet)
        case newMail: NewMail => broadcastToComet(newMail)
        case fetch: FetchGMail => transferRequest(fetch)
    }
}


class InboxComet extends CometActor with StuffList
{
    val projectID: Box[Int] = S.attr("projectID").map(_.toInt)
    val topicID: Box[Int] = S.attr("topicID").map(_.toInt)
    val gmailListener = new GMailListener
    val username: String = currentUser.username.is

    override def render = NodeSeq.Empty

    override def localShutdown() {
        MailCenter ! UnregisterInboxComet(this)
    }

    override def lowPriority = {
        case request: FetchGMail => forwardAndTicket(request)
        case NewMail(username, stuffs) => updateInboxList(stuffs)
    }

    def updateInboxList(stuffs: List[Stuff])
    {
        partialUpdate(AppendHtml("inboxList", stuffs.flatMap(createStuffRow)))
    }

    def forwardAndTicket(request: FetchGMail)
    {
        MailCenter ! request
        Schedule.schedule(this, FetchGMail(currentUser), 5 minutes)
    }
    
    MailCenter ! RegisterInboxComet(this)
    this ! FetchGMail(currentUser)
}
