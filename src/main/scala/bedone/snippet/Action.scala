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

    def render = 
    {
        ClearClearable &
        ".contextTab" #> contexts.zipWithIndex.map { case (context, i) =>
            "li [class]" #> (if(i == 0) "active" else "") &
            "a *"        #> context.title
        }
    }
}
