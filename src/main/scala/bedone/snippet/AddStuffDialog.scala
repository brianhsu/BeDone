package org.bedone.snippet

import org.bedone.model._

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw

import scala.xml.NodeSeq
import scala.xml.Text
import net.liftweb.record.Record

trait ModalDialog {
    protected val modalID: String
    protected val buttonID: String

    def hideModal: JsCmd = JsRaw("""$('#%s').modal('hide')""".format(modalID))
    def resetButton: JsCmd = JsRaw("""$('#%s').button('reset')""".format(buttonID))
}

class AddStuffDialog(postAction: => JsCmd) extends AjaxForm[Stuff] with ModalDialog
{
    override protected val modalID = "addStuffModal"
    override protected val buttonID = "addStuffButton"
    override protected val formID = Some("addStuffForm")

    override protected val record = Stuff.createRecord
    override protected val fields = List(record.title, record.description, record.deadline)

    private var topics: List[String] = Nil
    private var projects: List[String] = Nil

    def saveAndClose(): JsCmd = {

        record.saveTheRecord().foreach { stuff =>
            stuff.addTopics(topics)
            stuff.addProjects(projects)
        }

        hideModal & reInitForm & resetButton
    }

    def addStuff(): JsCmd =
    {
        record.userID.setBox(CurrentUser.get.map(_.idField.is))
        record.validate match {
            case Nil    => saveAndClose()
            case errors => errors.map(showFieldError) & resetButton
        }
    }


    def topicForm = tagsInput("stuffTopic", "主題", "新增主題", 
                              "/autocomplete/topic") 
    {
        value: String => 
            this.topics = value.split(",").map(_.trim).filter(_.length > 0).toList
    }

    def projectForm = tagsInput("stuffProject", "專案", "新增專案", 
                              "/autocomplete/project") 
    {
        value: String => 
            this.projects = value.split(",").map(_.trim).filter(_.length > 0).toList
    }

    override def cssBinding = super.cssBinding ++ List(topicForm, projectForm)
    override def reInitForm = {
        super.reInitForm & cleanTag("stuffTopic") & cleanTag("stuffProject")
    }

    def render = {
        ".modal-body *" #> this.toForm &
        ".close" #> SHtml.ajaxButton("×", reInitForm _) &
        ".close-link" #> SHtml.a(reInitForm _, Text("取消"), "href" -> "javascript:void(0)") &
        "#addStuffButton" #> SHtml.ajaxButton(Text("新增"), addStuff _)
    }
}

