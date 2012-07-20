package org.bedone.model

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record

import net.liftweb.record.field.LongField
import net.liftweb.record.field.StringField
import net.liftweb.record.field.PasswordField
import net.liftweb.record.field.OptionalEmailField
import net.liftweb.record.field.EmailField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._
import net.liftweb.common.Full

import org.squeryl.annotations.Column
import org.squeryl.Schema

object Bookstore extends Schema {
    val publishers = table[Publisher]("publishers")
}

object Publisher extends Publisher with MetaRecord[Publisher]
class Publisher private () extends Record[Publisher] with KeyedRecord[Long] {
    def meta = Publisher

    @Column(name="id")
    val idField = new LongField(this, 1)
    val name = new StringField(this, "") {
        override def validations = 
            valMinLen(6, "帳號至少要六個字")_ :: super.validations
    }
    val email = new EmailField(this, 255)
    val semail = new OptionalEmailField(this, 255)
    val password = new PasswordField(this)
}


