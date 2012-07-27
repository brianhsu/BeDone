package org.bedone.model

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.BooleanField
import net.liftweb.squerylrecord.RecordTypeMode._

object Delegated extends Delegated with MetaRecord[Delegated]
class Delegated extends Record[Delegated]
{
    def meta = Delegated

    val stuffID = new IntField(this)
    val contactID = new IntField(this)

    val hasInformed = new BooleanField(this, false)
    val hasRespond = new BooleanField(this, false)
}


