package org.bedone.model

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.squerylrecord.KeyedRecord

import net.liftweb.record.field.IntField
import net.liftweb.record.field.BooleanField
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column

object Action extends Action with MetaRecord[Action]
class Action extends Record[Action] with KeyedRecord[Int]
{
    def meta = Action
    
    @Column(name="stuffID")
    val idField = new IntField(this)
    def stuffID = idField

    val isDone = new BooleanField(this, false)
}
