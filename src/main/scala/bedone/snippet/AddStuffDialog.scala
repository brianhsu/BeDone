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

    def saveAndClose(): JsCmd = {

        record.saveTheRecord().foreach { savedRecord => 
            val stuffTopic = StuffTopic.createRecord.stuffID(record.idField.is)
            topics.foreach(topic => stuffTopic.topic(topic).saveTheRecord)
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

    def topicForm = {

        val fieldID = "stuffTopic"
        val messageID = fieldID + "_msg"
        val tagID = fieldID + "_tag"

        def tagItJS = """
            $('#%s').tagsInput({
                autocomplete_url:'http://localhost:8081/autocomplete/topic',
                autocomplete:{selectFirst:true,width:'100px',autoFill:true},
                defaultText: '新增主題',
                width: 210,
                height: 20,
                onChange: function() { $('#%s').blur() }
            });
        """.format(tagID, tagID)

        def ajaxTest(value: String) = {
            this.topics = value.split(",").map(_.trim).filter(_.trim.length > 0).toList
            Noop
        }

        ".control-group [id]" #> fieldID &
        ".control-group *" #> (
            ".control-label *" #> "主題" &
            ".help-inline [id]" #> messageID &
            ".help-inline *" #> "使用逗號分隔，按下 Enter 確認" &
            "input" #> (
                SHtml.textAjaxTest("", doNothing _, ajaxTest _, "id" -> tagID) ++
                <script type="text/javascript">{tagItJS}</script>
            )
        )
    }

    override def cssBinding = super.cssBinding :+ topicForm
    override def reInitForm = super.reInitForm & """$('#stuffTopic_tag').importTags("")"""

    def render = {
        ".modal-body *" #> this.toForm &
        ".close" #> SHtml.ajaxButton("×", reInitForm _) &
        ".close-link" #> SHtml.a(reInitForm _, Text("取消"), "href" -> "javascript:void(0)") &
        "#addStuffButton" #> SHtml.ajaxButton(Text("新增"), addStuff _)
    }
}

