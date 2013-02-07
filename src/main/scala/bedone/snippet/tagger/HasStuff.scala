package org.bedone.snippet

import org.bedone.model._

trait HasStuff
{
    protected implicit def optFromStr(x: String) = Option(x).filterNot(_.trim.length == 0)

    def stuff: Option[Stuff]
}

