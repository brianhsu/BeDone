package org.bedone.model

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.BooleanField
import net.liftweb.squerylrecord.RecordTypeMode._

object Action extends Action with MetaRecord[Action]
class Action extends Record[Action]
{
    def meta = Action

    val stuffID = new IntField(this)
    val isDone = new BooleanField(this, false)
}


