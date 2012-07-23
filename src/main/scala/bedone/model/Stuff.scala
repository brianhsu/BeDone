package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.util.Helpers.tryo

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.LongField
import net.liftweb.record.field.StringField
import net.liftweb.record.field.DateTimeField
import net.liftweb.record.field.OptionalDateTimeField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column

object Stuff extends Stuff with MetaRecord[Stuff]
{
    def findByUser(user: User): Box[List[Stuff]] = inTransaction {
        tryo {
            BeDoneSchema.stuffs.where(_.userID === user.idField).toList
        }
    }
}

class Stuff extends Record[Stuff] with KeyedRecord[Long] {
    def meta = Stuff

    @Column(name="id")
    val idField = new LongField(this, 1)

    val userID = new LongField(this, 1)
    val title = new StringField(this, "")
    val description = new StringField(this, "")
    val createTime = new DateTimeField(this)
    val deadline = new OptionalDateTimeField(this)
}

