package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.util.Helpers.tryo

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.squerylrecord.KeyedRecord

import net.liftweb.record.field.IntField
import net.liftweb.record.field.BooleanField
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column

object Action extends Action with MetaRecord[Action]
{
    def findByUser(user: User): Box[List[Action]] = inTransaction {
        tryo {
            from(BeDoneSchema.stuffs, BeDoneSchema.actions) ( (stuff, action) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.stuffType === StuffType.Action and
                    action.idField === stuff.idField
                ) 
                select(action) 
                orderBy(stuff.deadline desc)
            ).toList
        }
    }
}

class Action extends Record[Action] with KeyedRecord[Int]
{
    def meta = Action
    
    @Column(name="stuffID")
    val idField = new IntField(this)
    def stuffID = idField

    val isDone = new BooleanField(this, false)
}
