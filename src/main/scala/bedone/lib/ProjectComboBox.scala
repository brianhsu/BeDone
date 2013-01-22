package org.bedone.lib

import org.bedone.model._

import net.liftmodules.combobox._

import net.liftweb.http.S
import net.liftweb.http.js.JE.Str
import net.liftweb.http.js.JsExp
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd

object ProjectComboBox {
    val defaultOptions = List(
        "placeholder" -> Str(
            """<i class="icon-folder-open"></i> """ +
            S.?("Select project")
        )
    )
}

abstract class ProjectComboBox(options: List[(String, JsExp)] = ProjectComboBox.defaultOptions) extends ComboBox(None, true, options) {

    private lazy val currentUser = CurrentUser.get.get

    def addProject(project: Project): JsCmd

    override def onSearching(term: String): List[ComboItem] = {
        Project.findByUser(currentUser).openOr(Nil)
               .filter(p => p.title.is.contains(term))
               .map(p => ComboItem(p.idField.toString, p.title.is))
    }

    override def onItemSelected(item: Option[ComboItem]): JsCmd = {
        item match {
            case None => Noop
            case Some(selected) =>
                val project = Project.findByID(selected.id.toInt).get
                addProject(project)
        }
    }

    override def onItemAdded(name: String): JsCmd = {
        val userID = CurrentUser.get.get.idField.is
        val topic = Project.createRecord.userID(userID).title(name)
        addProject(topic)
    }
}

