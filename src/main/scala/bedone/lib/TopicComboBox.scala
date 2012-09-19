package org.bedone.lib

import org.bedone.model._

import net.liftmodules.combobox._

import net.liftweb.http.js.JE.Str
import net.liftweb.http.js.JsExp
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd

object TopicComboBox {
    val defaultOptions = List(
        "placeholder" -> Str("""<i class="icon-tag"></i> 請選擇所屬的主題""")
    )
}

abstract class TopicComboBox(options: List[(String, JsExp)] = TopicComboBox.defaultOptions) extends ComboBox(None, true, options) {
    
    private lazy val currentUser = CurrentUser.get.get

    def addTopic(topic: Topic): JsCmd

    override def onSearching(term: String): List[ComboItem] = {
        Topic.findByUser(currentUser).openOr(Nil)
               .filter(t => t.title.is.contains(term))
               .map(t => ComboItem(t.idField.toString, t.title.is))
    }
    
    override def onItemSelected(item: Option[ComboItem]): JsCmd = {
        item match {
            case None => Noop
            case Some(selected) =>
                val topic = Topic.findByID(selected.id.toInt).get
                addTopic(topic)
        }
    }

    override def onItemAdded(name: String): JsCmd = {
        val userID = CurrentUser.get.get.idField.is
        val topic = Topic.createRecord.userID(userID).title(name)
        addTopic(topic)
    }
}

