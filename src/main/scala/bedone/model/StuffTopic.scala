package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.common.Full

import net.liftweb.util.Helpers.tryo

import net.liftweb.util.FieldError

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.LongField
import net.liftweb.record.field.StringField
import net.liftweb.record.field.TextareaField
import net.liftweb.record.field.DateTimeField
import net.liftweb.record.field.OptionalDateTimeField
import net.liftweb.record.field.BooleanField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column
import net.liftweb.util.Helpers.tryo
import java.io.StringReader
import java.io.StringWriter

object StuffTopic extends StuffTopic with MetaRecord[StuffTopic]
{
    def findByUser(user: User) = inTransaction (tryo {
        from(BeDoneSchema.stuffs, BeDoneSchema.stuffTopics) { (stuff, topic) =>
            where(stuff.userID === user.idField and stuff.idField === topic.stuffID)
            select(topic)
        }.toList
    })
}

class StuffTopic extends Record[StuffTopic] 
{
    def meta = StuffTopic

    val stuffID = new LongField(this)
    val topic = new StringField(this, "") {
        override def validations = valMinLen(1, "此為必填欄位")_ :: super.validations
    }

    override def saveTheRecord = inTransaction ( tryo {
        import BeDoneSchema.stuffTopics

        val oldTopics = stuffTopics.where(t => t.stuffID === stuffID and t.topic === topic)

        oldTopics.toList match {
            case Nil => stuffTopics.insert(this)
            case xs  => this
        }
    })

}

