package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds._

import TagButton.Implicit._

trait ContextTagger extends JSImplicit
{
    var currentContexts: List[Context]
    val contextTagContainers: List[String]

    def onContextClick(buttonID: String, context: Context) = Noop

    def onContextRemove(buttonID: String, context: Context): JsCmd = 
    {
        currentContexts = currentContexts.filterNot(_ == context)
        FadeOutAndRemove.byClassName(context.className)
    }

    def appendContextTagJS(context: Context) = 
    {
        contextTagContainers.map { htmlID => 
            AppendHtml(htmlID, context.editButton(onContextClick, onContextRemove))
        }
    }

    def createContextTags(containerID: String) =
    {
        val contextCombobox = new ContextComboBox{
            def addContext(context: Context) = {
                currentContexts.map(_.title.is).contains(context.title.is) match {
                    case true  => this.clear
                    case false =>
                        currentContexts ::= context
                        this.clear & appendContextTagJS(context)
                }
            }
        }

        ".contextCombo"     #> contextCombobox.comboBox &
        ".contextTags [id]" #> containerID &
        ".contextTags" #> (
            "span" #> currentContexts.map(_.editButton(onContextClick, onContextRemove))
        )
    }

}

