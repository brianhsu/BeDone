package org.bedone.lib

import org.bedone.model._

import net.liftmodules.combobox._

import net.liftweb.http.js.JE.Str
import net.liftweb.http.js.JsExp
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd

object ContextComboBox {
    val defaultOptions = List(
        "placeholder" -> Str("""<i class="icon-briefcase"></i> 請選擇所屬的情境""")
    )
}

abstract class ContextComboBox(options: List[(String, JsExp)] = ContextComboBox.defaultOptions) extends ComboBox(None, true, options) {
    
    private lazy val currentUser = CurrentUser.get.get

    def addContext(context: Context): JsCmd

    override def onSearching(term: String): List[ComboItem] = {
        Context.findByUser(currentUser).openOr(Nil)
               .filter(c => c.title.is.contains(term))
               .map(c => ComboItem(c.idField.toString, c.title.is))
    }
    
    override def onItemSelected(item: Option[ComboItem]): JsCmd = {
        item match {
            case None => Noop
            case Some(selected) =>
                val context = Context.findByID(selected.id.toInt).get
                addContext(context)
        }
    }

    override def onItemAdded(name: String): JsCmd = {
        val userID = CurrentUser.get.get.idField.is
        val context = Context.createRecord.userID(userID).title(name)
        addContext(context)
    }
}

