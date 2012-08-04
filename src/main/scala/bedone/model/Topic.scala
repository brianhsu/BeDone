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

object Topic extends Topic with MetaRecord[Topic]
{
    import net.liftweb.common.Box._

    def findByID(id: Int): Box[Topic] = 
        BeDoneSchema.topics.where(_.idField === id).headOption

    def findByUser(user: User): Box[List[Topic]] = findByUser(user.idField.is)
    def findByUser(userID: Int): Box[List[Topic]] = tryo{
        BeDoneSchema.topics.where(_.userID === userID).toList
    }

    def findByTitle(userID: Int, title: String): Box[Topic] = 
        BeDoneSchema.topics.where(t => t.userID === userID and t.title === title).headOption
}

class Topic extends Record[Topic] with KeyedRecord[Int]
{
    def meta = Topic

    @Column(name="id")
    val idField = new IntField(this)
    val userID = new IntField(this)
    val title = new StringField(this, "")
    val description = new TextareaField(this, 1000)

    override def saveTheRecord() = tryo(BeDoneSchema.topics.insert(this))

    def stuffs = { 
        BeDoneSchema.stuffTopics.right(this)
                    .filter(_.stuffType.is == StuffType.Stuff)toList
    }

    def addStuff(stuff: Stuff) = BeDoneSchema.stuffTopics.right(this).associate(stuff)
}
