package org.bedone.model

import net.liftweb.http.CometActor

case class FetchGMail(user: User, comet: CometActor)
case class NewStuffs(mails: List[Stuff])

