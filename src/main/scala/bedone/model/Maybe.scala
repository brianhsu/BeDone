package org.bedone.model

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.OptionalDateTimeField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

object Maybe extends Maybe with MetaRecord[Maybe]

class Maybe extends Record[Maybe]
{
    def meta = Maybe

    val stuffID = new IntField(this)
    val tickler = new OptionalDateTimeField(this)
}

