package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._

import java.text.SimpleDateFormat

class ProjectTable extends JSImplicit
{
    val currentUser = CurrentUser.is.get
    def projects = Project.findByUser(currentUser).openOr(Nil)

    def stripZero(size: Int): String = if (size > 0) size.toString else ""

    def isTrashOrDone(stuff: Stuff) = {
        stuff.isTrash.is || Action.findByID(stuff.idField.is).map(_.isDone.is).getOrElse(false)
    }

    def deleteProject(project: Project)(): JsCmd = {
        Project.delete(project)
        S.notice("已刪除「%s」" format(project.title.is))
        FadeOutAndRemove("project" + project.idField.is)
    }

    def createProjectRow(project: Project) = {

        val stuffs = project.stuffs.filterNot(isTrashOrDone)
        val nextActions = project.nextActions.filterNot(isTrashOrDone)
        val delegateds = project.delegateds.filterNot(isTrashOrDone)
        val scheduleds = project.scheduleds.filterNot(isTrashOrDone)
        val maybes = project.maybes.filterNot(isTrashOrDone)
        val references = project.references.filterNot(isTrashOrDone)

        "tr [id]"       #> ("project" + project.idField.is) &
        ".name *"       #> project.title.is &
        ".inbox *"      #> stripZero(stuffs.size) &
        ".nextAction *" #> stripZero(nextActions.size) &
        ".delegated *"  #> stripZero(delegateds.size) &
        ".scheduled *"  #> stripZero(scheduleds.size) &
        ".maybe *"      #> stripZero(maybes.size) &
        ".reference *"  #> stripZero(references.size) &
        ".delete [onclick]" #> Confirm(
            "確定刪除「%s」嗎？這個動作無法還原喲！" format(project.title.is), 
            SHtml.ajaxInvoke(deleteProject(project))
        )

    }

    def render = {
        "tr" #> projects.map(createProjectRow)
    }
}
