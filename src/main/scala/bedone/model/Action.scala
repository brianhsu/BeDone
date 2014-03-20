package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.util.Helpers.tryo

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.squerylrecord.KeyedRecord

import net.liftweb.record.field.IntField
import net.liftweb.record.field.BooleanField
import net.liftweb.record.field.OptionalDateTimeField

import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column
import java.text.SimpleDateFormat

import StuffType.StuffType

case class ActionT(stuff: Stuff, action: Action)

object Action extends Action with MetaRecord[Action]
{
    def toActionT(t: (Stuff, Action)) = ActionT(t._1, t._2)

    def findByID(id: Int): Box[Action] = 
        tryo(BeDoneSchema.actions.where(_.idField === id).single)

    def findByTopic(user: User, topicID: Int) = {

        import BeDoneSchema._

        tryo {
            from(stuffs, actions, stuffTopics) ( (stuff, action, stuffTopic) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.stuffType === StuffType.Action and
                    stuff.isTrash === false and
                    action.idField === stuff.idField and
                    stuffTopic.stuffID === stuff.idField and 
                    stuffTopic.topicID === topicID
                ) 
                select(stuff, action) 
                orderBy(action.doneTime desc, stuff.deadline desc, stuff.createTime)
            ).map(toActionT).toList
        }
    }


    def findByProject(user: User, projectID: Int) = {

        import BeDoneSchema._

        tryo {
            from(stuffs, actions, stuffProjects) ( (stuff, action, stuffProject) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.stuffType === StuffType.Action and
                    stuff.isTrash === false and
                    action.idField === stuff.idField and
                    stuffProject.stuffID === stuff.idField and 
                    stuffProject.projectID === projectID
                ) 
                select(stuff, action) 
                orderBy(action.doneTime desc, stuff.deadline desc, stuff.createTime)
            ).map(toActionT).toList
        }
    }

    def findByUser(user: User, stuffType: StuffType = StuffType.Action): Box[List[ActionT]] = 
    {
        tryo {
            from(BeDoneSchema.stuffs, BeDoneSchema.actions) ( (stuff, action) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.stuffType === stuffType and
                    stuff.isTrash === false and
                    action.idField === stuff.idField
                ) 
                select(stuff, action) 
                orderBy(action.doneTime desc, stuff.deadline desc, stuff.createTime)
            ).map(toActionT).toList
        }
    }
}

class Action extends Record[Action] with KeyedRecord[Int]
{
    def meta = Action
    
    @Column(name="stuffID")
    val idField = new IntField(this)
    val isDone = new BooleanField(this, false)
    val doneTime = new OptionalDateTimeField(this)

    def stuff = Stuff.findByID(idField.is).get
    def contexts = BeDoneSchema.actionContexts.left(this).toList

    lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    def removeContext(context: Context) = 
        BeDoneSchema.actionContexts.left(this).dissociate(context)

    def addContext(context: Context) = {
        if (!context.isPersisted) { context.saveTheRecord() }
        BeDoneSchema.actionContexts.left(this).associate(context)
    }

    def setContexts(contexts: List[Context]) = {
        val shouldRemove = this.contexts.filterNot(contexts.contains)
        val shouldAdd = contexts.filterNot(this.contexts.contains)

        shouldRemove.foreach(removeContext)
        shouldAdd.foreach(addContext)
    }

    def formatDoneTime: Option[String] = isDone.is match {
        case true => doneTime.is.map(calendar => dateTimeFormatter.format(calendar.getTime))
        case false => None
    }

    def formatDeadline: Option[String] = isDone.is match {
        case true => None
        case false => stuff.deadline.is.map(calendar => dateFormatter.format(calendar.getTime))
    }

    override def saveTheRecord() = tryo {
        this.isPersisted match {
            case true  => BeDoneSchema.actions.update(this)
            case false => BeDoneSchema.actions.insert(this)
        }

        this
    }
}
