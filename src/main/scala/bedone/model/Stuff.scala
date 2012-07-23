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
    val createTime = new DateTimeField(this)

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
}

