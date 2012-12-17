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
import scala.xml.NodeSeq

class ProjectTable extends Table with JSImplicit
{
    val currentUser = CurrentUser.is.get
    def projects = Project.findByUser(currentUser).openOr(Nil)

    def editProject(project: Project)(value: String): JsCmd = {
        val editForm = new EditProjectForm(project, project => {
            val rowID = "#project" + project.idField.is

            FadeOutWithCallback("editProjectForm") {
                "$('%s .name').text('%s')".format(rowID, project.title.is) &
                "$('%s .name').attr('data-original-title', '%s')".format(rowID, project.description.is)
            }
        })

        JqSetHtml("editProjectHolder", editForm.toForm)
    }

    def deleteProject(project: Project)(): JsCmd = {
        Project.delete(project)
        S.notice(S.?("Project '%s' is deleted.") format(project.title.is))
        FadeOutAndRemove("project" + project.idField.is)
    }

    def createProjectRow(project: Project) = {

        val stuffs = project.stuffs.filterNot(isDone)
        val nextActions = project.nextActions.filterNot(isDone)
        val delegateds = project.delegateds.filterNot(isDone)
        val scheduleds = project.scheduleds.filterNot(isDone)
        val maybes = project.maybes.filterNot(isDone)
        val references = project.references.filterNot(isDone)

        "tr [id]"       #> ("project" + project.idField.is) &
        ".name *"       #> project.title.is &
        ".name [href]"  #> ("/project/" + project.idField.is) &
        ".inbox *"      #> stripZero(stuffs.size) &
        ".nextAction *" #> stripZero(nextActions.size) &
        ".delegated *"  #> stripZero(delegateds.size) &
        ".scheduled *"  #> stripZero(scheduleds.size) &
        ".maybe *"      #> stripZero(maybes.size) &
        ".reference *"  #> stripZero(references.size) &
        ".name [data-original-title]" #> project.description.is &
        ".delete [onclick]" #> Confirm(
            S.?("Are you sure to delete project '%s' permanently?") format(project.title.is), 
            SHtml.ajaxInvoke(deleteProject(project))
        ) &
        ".edit [onclick]" #> SHtml.onEvent(editProject(project))

    }

    def render = {
        "tr" #> projects.map(createProjectRow)
    }
}
