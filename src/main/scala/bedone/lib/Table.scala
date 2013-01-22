package org.bedone.lib

import org.bedone.model._

trait Table
{
    def stripZero(size: Int): String = if (size > 0) size.toString else ""

    def isDone(stuff: Stuff) = {
        Action.findByID(stuff.idField.is).map(_.isDone.is).getOrElse(false)
    }
}

