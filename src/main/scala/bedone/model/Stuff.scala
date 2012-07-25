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

class StuffTopic extends Record[StuffTopic] {
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

object Stuff extends Stuff with MetaRecord[Stuff]
{
    def findByUser(user: User): Box[List[Stuff]] = inTransaction {
        tryo {
            from(BeDoneSchema.stuffs)(table =>
                where(table.userID === user.idField) 
                select(table)
                orderBy(table.createTime asc)
            ).toList
        }
    }
}

class Stuff extends Record[Stuff] with KeyedRecord[Long] {
    def meta = Stuff

    @Column(name="id")
    val idField = new LongField(this, 1)
    val userID = new LongField(this)
    val createTime = new DateTimeField(this)
    val isTrash = new BooleanField(this, false)

    val title = new StringField(this, "") {
        override def displayName = "標題"
        override def validations = valMinLen(1, "此為必填欄位")_ :: super.validations
    }
    val description = new TextareaField(this, 1000) {
        override def displayName = "描述"
    }
    val deadline = new OptionalDateTimeField(this) {
        override def displayName = "期限"
        override def helpAsHtml = Full(scala.xml.Text("格式為 yyyy-MM-dd"))
    }

    def topics = inTransaction(tryo {
        BeDoneSchema.stuffTopics.where(_.stuffID === this.idField).map(_.topic.is).toList
    })

    def descriptionHTML = {
        import org.tautua.markdownpapers.Markdown
        val reader = new StringReader(description.is)
        val writer = new StringWriter
        val markdown = new Markdown
        markdown.transform(reader, writer)

        val rawHTML = "<div>" + writer.toString + "</div>"
        scala.xml.XML.loadString(rawHTML)
    }

    override def saveTheRecord() = inTransaction { tryo(BeDoneSchema.stuffs.insert(this)) }
}

