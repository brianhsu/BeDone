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

class ProjectDetail(project: Project) extends JSImplicit
{
    private var currentNavItem = "inbox"
    private val projectID = project.idField.is

    def template = currentNavItem match {
        case "inbox"      => "lift:embed?what=stuff/list?projectID=" + projectID
        case "nextAction" => "lift:embed?what=action/list?projectID=" + projectID
        case "delegated"  => "lift:embed?what=delegated/list?projectID=" + projectID
        case "scheduled"  => "lift:embed?what=scheduled/list?projectID=" + projectID
        case "maybe"      => "lift:embed?what=maybe/list?projectID=" + projectID
        case "reference"  => "lift:embed?what=reference/list?projectID=" + projectID
    }


    def updateContent(): JsCmd =
    {
        val content = <div data-lift={template}></div>

        "$('.navItem').removeClass('active')" &
        "$('.%s').addClass('active')".format(currentNavItem) &
        JqSetHtml("detailContent", content)
    }

    def switchNavItem(item: String) = {

        this.currentNavItem = item

        updateContent()
    }

    def render = {

        ".projectTitle *" #> project.title.is &
        ".inbox" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("inbox"))) &
        ".nextAction" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("nextAction"))) &
        ".delegated" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("delegated"))) &
        ".scheduled" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("scheduled"))) &
        ".maybe" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("maybe"))) &
        ".reference" #> ("a [onclick]" #> SHtml.onEvent(s => switchNavItem("reference"))) &
        "#detailContent *" #> <div data-lift={template}></div>
    }
}

class ProjectTable extends Table with JSImplicit
{
    val currentUser = CurrentUser.is.get
    def projects = Project.findByUser(currentUser).openOr(Nil)

    def editProject(project: Project)(value: String): JsCmd = {
        val editForm = new EditProjectForm(project, project => {
            val rowID = "#project" + project.idField.is

            FadeOutAndRemove("editProjectForm") &
            "$('%s .name').text('%s')".format(rowID, project.title.is) &
            "$('%s .name').attr('data-original-title', '%s')".format(rowID, project.description.is)
        })

        JqSetHtml("editProjectHolder", editForm.toForm)
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
        ".name [href]"  #> ("/project/" + project.idField.is) &
        ".inbox *"      #> stripZero(stuffs.size) &
        ".nextAction *" #> stripZero(nextActions.size) &
        ".delegated *"  #> stripZero(delegateds.size) &
        ".scheduled *"  #> stripZero(scheduleds.size) &
        ".maybe *"      #> stripZero(maybes.size) &
        ".reference *"  #> stripZero(references.size) &
        ".name [data-original-title]" #> project.description.is &
        ".delete [onclick]" #> Confirm(
            "確定刪除「%s」嗎？這個動作無法還原喲！" format(project.title.is), 
            SHtml.ajaxInvoke(deleteProject(project))
        ) &
        ".edit [onclick]" #> SHtml.onEvent(editProject(project))

    }

    def render = {
        "tr" #> projects.map(createProjectRow)
    }
}
