package org.bedone.model

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.OptionalIntField
import net.liftweb.record.field.OptionalStringField
import net.liftweb.record.field.DateTimeField
import net.liftweb.squerylrecord.RecordTypeMode._

object Scheduled extends Scheduled with MetaRecord[Scheduled]
class Scheduled extends Record[Scheduled]
{
    def meta = Scheduled

    val actionID = new IntField(this)
    val startTime = new DateTimeField(this)
    val durationInMinute = new OptionalIntField(this)
    val location = new OptionalStringField(this, 255)
}


