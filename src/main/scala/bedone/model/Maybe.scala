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

object Maybe extends Maybe with MetaRecord[Maybe]
{
    def findByID(id: Int): Box[Maybe] = inTransaction (
        BeDoneSchema.maybes.where(_.idField === id).headOption
    )

    def findByUser(user: User): Box[List[Maybe]] = findByUser(user.idField.is)
    def findByUser(userID: Int): Box[List[Maybe]] = inTransaction(tryo{
        BeDoneSchema.maybes.where(_.userID === userID).toList
    })
}

class Maybe extends Record[Maybe] with KeyedRecord[Int] 
{
    def meta = Maybe

    @Column(name="id")
    val idField = new IntField(this)
    val userID = new IntField(this)

    val createTime = new DateTimeField(this)
    val updateTime = new DateTimeField(this)
    val tickler = new OptionalDateTimeField(this)

    val isTrash = new BooleanField(this, false)

    val title = new StringField(this, "") {
        override def displayName = "標題"
        override def validations = valMinLen(1, "此為必填欄位")_ :: super.validations
    }

    val description = new TextareaField(this, 1000) {
        override def displayName = "描述"
    }
}

