package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.SHtml
import net.liftweb.util.ClearClearable

class Action
{
    val currentUser = CurrentUser.get.get
    val contexts = Context.findByUser(currentUser).openOr(Nil)
    def doneActions = Action.findByUser(currentUser).openOr(Nil).filter(_.isDone.is)
    def notDoneActions = Action.findByUser(currentUser).openOr(Nil).filterNot(_.isDone.is)

    def render = 
    {
        println(doneActions)
        println(notDoneActions)

        ClearClearable &
        ".contextTab" #> contexts.zipWithIndex.map { case (context, i) =>
            "li [class]" #> (if(i == 0) "active" else "") &
            "a *"        #> context.title
        }
    }
}
