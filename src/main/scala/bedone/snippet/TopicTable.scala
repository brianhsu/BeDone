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

trait Table
{
    def stripZero(size: Int): String = if (size > 0) size.toString else ""

    def isTrashOrDone(stuff: Stuff) = {
        stuff.isTrash.is || Action.findByID(stuff.idField.is).map(_.isDone.is).getOrElse(false)
    }
}

class TopicTable extends Table with JSImplicit 
{
    val currentUser = CurrentUser.is.get
    def topics = Topic.findByUser(currentUser).openOr(Nil)

    def editTopic(topic: Topic)(value: String): JsCmd = {
        
        def postAction(topic: Topic) = {
            val rowID = "#topic" + topic.idField.is

            FadeOutAndRemove("editTopicForm") &
            "$('%s .name').text('%s')".format(rowID, topic.title.is) &
            "$('%s .name').attr('data-original-title', '%s')".format(
                rowID, topic.description.is
            )
        }

        JqSetHtml("editTopicHolder", new EditTopicForm(topic, postAction).toForm)
    }

    def deleteTopic(topic: Topic)() = {
        Topic.delete(topic)
        S.notice("已刪除「%s」" format(topic.title.is))
        FadeOutAndRemove("topic" + topic.idField.is)
    }

    def createTopicRow(topic: Topic) = {

        val stuffs = topic.stuffs.filterNot(isTrashOrDone)
        val nextActions = topic.nextActions.filterNot(isTrashOrDone)
        val delegateds = topic.delegateds.filterNot(isTrashOrDone)
        val scheduleds = topic.scheduleds.filterNot(isTrashOrDone)
        val maybes = topic.maybes.filterNot(isTrashOrDone)
        val references = topic.references.filterNot(isTrashOrDone)

        "tr [id]"       #> ("topic" + topic.idField.is) &
        ".name *"       #> topic.title.is &
        ".inbox *"      #> stripZero(stuffs.size) &
        ".nextAction *" #> stripZero(nextActions.size) &
        ".delegated *"  #> stripZero(delegateds.size) &
        ".scheduled *"  #> stripZero(scheduleds.size) &
        ".maybe *"      #> stripZero(maybes.size) &
        ".reference *"  #> stripZero(references.size) &
        ".name [data-original-title]" #> topic.description.is &
        ".edit [onclick]" #> SHtml.onEvent(editTopic(topic)) &
        ".delete [onclick]" #> Confirm(
            "確定刪除「%s」嗎？這個動作無法還原喲！" format(topic.title.is), 
            SHtml.ajaxInvoke(deleteTopic(topic))
        )
    }

    def render = {
        "tr" #> topics.map(createTopicRow)
    }
}
