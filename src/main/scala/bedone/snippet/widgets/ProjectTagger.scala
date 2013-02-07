package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.util.Helpers._

import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds._

import TagButton.Implicit._

trait ProjectTagger extends JSImplicit
{
    var currentProjects: List[Project]
    val projectTagContainers: List[String]

    private def onProjectClick(buttonID: String, project: Project) = Noop
    private def onProjectRemove(buttonID: String, project: Project): JsCmd = {

        currentProjects = currentProjects.filterNot(_ == project)
        FadeOutAndRemove.byClassName(project.className)
    }

    private def appendProjectTagJS(project: Project) =
    {
        projectTagContainers.map { htmlID =>
            AppendHtml(htmlID, project.editButton(onProjectClick, onProjectRemove))
        }
    }

    def createProjectTags(containerID: String) =
    {
        val projectCombobox = new ProjectComboBox {
            def addProject(project: Project) = {
                currentProjects.map(_.title.is).contains(project.title.is) match {
                    case true  => this.clear
                    case false =>
                        currentProjects ::= project
                        this.clear & appendProjectTagJS(project)
                }
            }
        }

        ".projectCombo"     #> projectCombobox.comboBox &
        ".projectTags [id]" #> containerID &
        ".projectTags" #> (
            "span" #> currentProjects.map(_.editButton(onProjectClick, onProjectRemove))
        )
    }

}

