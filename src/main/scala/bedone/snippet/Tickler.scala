package org.bedone.snippet

import org.bedone.model._

import net.liftweb.common.Box
import net.liftweb.common.Empty
import net.liftweb.common.Full
import net.liftweb.common.Failure

import net.liftweb.util.Helpers._
import net.liftweb.util.PassThru

import net.liftweb.http.SHtml
import net.liftweb.http.S

import scala.xml.NodeSeq

class Tickler
{
    private lazy val currentUser = CurrentUser.get

    def processTickler(user: User) = {
        val outdated = Maybe.outdated(user).openOr(Nil)
        outdated.map(_.stuff.reInbox())
    }

    def tickle = {
        currentUser.foreach(processTickler)
        PassThru
    }
}
