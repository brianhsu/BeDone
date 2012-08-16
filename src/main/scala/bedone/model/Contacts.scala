package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.util.Helpers.tryo

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.StringField
import net.liftweb.record.field.OptionalEmailField
import net.liftweb.record.field.OptionalStringField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column

object Contact extends Contact with MetaRecord[Contact]
{
    def findByID(id: Int): Box[Contact] = tryo {
        BeDoneSchema.contacts.where(_.idField === id).single
    }

    def findByUser(user: User): Box[List[Contact]] = tryo {
        BeDoneSchema.contacts.where(_.userID === user.idField).toList
    }

}

class Contact extends Record[Contact] with KeyedRecord[Int]
{
    def meta = Contact

    @Column(name="id")
    val idField = new IntField(this)
    val userID = new IntField(this)
    val name = new StringField(this, "")

    val email = new OptionalEmailField(this, 100)
    val address = new OptionalStringField(this, 255)
    val phone = new OptionalStringField(this, 20)
}
