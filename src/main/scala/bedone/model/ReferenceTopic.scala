package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.common.Full

import net.liftweb.util.Helpers.tryo

import net.liftweb.util.FieldError

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
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

object ReferenceTopic extends ReferenceTopic with MetaRecord[ReferenceTopic]
{
    def findByReference(reference: Reference) = inTransaction(tryo{
        BeDoneSchema.referenceTopics.where { 
            _.referenceID === reference.idField
        }.map(_.reference).toList   
    })
}

class ReferenceTopic extends Record[ReferenceTopic] 
{
    def meta = ReferenceTopic

    val referenceID = new IntField(this)
    val topicID = new IntField(this)

    def topic: Topic = Topic.findByID(topicID.is).open_!
    def reference: Reference = Reference.findByID(referenceID.is).open_!

    override def saveTheRecord = inTransaction(tryo(BeDoneSchema.referenceTopics.insert(this)))

}

