package org.bedone.lib

import org.bedone.model._

trait Table
{
    def stripZero(size: Int): String = if (size > 0) size.toString else ""

    def isTrashOrDone(stuff: Stuff) = {
        stuff.isTrash.is || Action.findByID(stuff.idField.is).map(_.isDone.is).getOrElse(false)
    }
}

