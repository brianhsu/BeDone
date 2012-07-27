package org.bedone.model

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.StringField
import net.liftweb.record.field.OptionalEmailField
import net.liftweb.record.field.OptionalStringField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column

object Contacts extends Contacts with MetaRecord[Contacts]
class Contacts extends Record[Contacts] with KeyedRecord[Int]
{
    def meta = Contacts

    @Column(name="id")
    val idField = new IntField(this, 1)
    val userID = new IntField(this)
    val name = new StringField(this, "")

    val email = new OptionalEmailField(this, 100)
    val address = new OptionalStringField(this, 255)
    val phone = new OptionalStringField(this, 20)
}
