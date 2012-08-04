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

import StuffType.StuffType

object Action extends Action with MetaRecord[Action]
{
    def findByID(id: Int): Box[Action] = inTransaction {
        tryo { BeDoneSchema.actions.where(_.idField === id).single }
    }

    def findByUser(user: User, stuffType: StuffType = StuffType.Action): Box[List[Action]] = 
    {
        inTransaction(tryo{
            from(BeDoneSchema.stuffs, BeDoneSchema.actions) ( (stuff, action) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.stuffType === stuffType and
                    action.idField === stuff.idField
                ) 
                select(action) 
                orderBy(stuff.deadline desc)
            ).toList
        })
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
    def topics = stuff.topics
    def projects = stuff.projects
    def contexts = inTransaction(BeDoneSchema.actionContexts.left(this).toList)

    def removeContext(context: Context) = inTransaction {
        BeDoneSchema.actionContexts.left(this).dissociate(context)
    }

    def addContext(context: Context) = inTransaction { 
        if (!context.isPersisted) { context.saveTheRecord() }
        BeDoneSchema.actionContexts.left(this).associate(context)
    }

    def setContexts(contexts: List[Context]) = inTransaction {
        val shouldRemove = this.contexts.filterNot(contexts.contains)
        val shouldAdd = contexts.filterNot(this.contexts.contains)

        shouldRemove.foreach(removeContext)
        shouldAdd.foreach(addContext)
    }

    override def saveTheRecord() = inTransaction(tryo{
        this.isPersisted match {
            case true  => BeDoneSchema.actions.update(this)
            case false => BeDoneSchema.actions.insert(this)
        }

        this
    })
}
