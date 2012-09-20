package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.common.Empty

import net.liftweb.util.Helpers.tryo
import net.liftweb.util.Helpers.hashHex

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.StringField
import net.liftweb.record.field.BooleanField
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

    def findByName(user: User, name: String): Box[Contact] = tryo {
        BeDoneSchema.contacts.where( contact =>
            contact.userID === user.idField and
            contact.name === name
        ).single
    }

    def findByUser(user: User): Box[List[Contact]] = tryo {
        from(BeDoneSchema.contacts) { contact => 
            where(contact.userID === user.idField).
            select(contact).
            orderBy(contact.name)
        }.toList
    }

    def findByGoogleID(userID: Int, googleID: String): Box[Contact] = tryo(classOf[Exception]) {
        from(BeDoneSchema.contacts) { contact => 
            where(
                contact.userID === userID and
                contact.googleID === Some(googleID)
            ).
            select(contact)
        }.head
    }

    def paramParser(param: String): Box[Contact] = tryo {
        inTransaction {
            CurrentUser.is.flatMap { user => 
                findByID(param.toInt).filter(_.userID.is == user.idField.is)
            }.get
        }
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
    val googleID = new OptionalStringField(this, 255)

    val isTrash = new BooleanField(this, false)

    def className = "topic%d%s" format (userID.is, hashHex(name.is))

    override def saveTheRecord() = tryo {

        val googleContact = googleID.is.flatMap(gID => Contact.findByGoogleID(userID.is, gID))

        if (googleContact.isDefined && !this.isPersisted) {
            
            val contact = googleContact.get
            contact.name(this.name.is).email(this.email.is)
                   .address(this.address.is).phone(this.phone.is)
                   .isTrash(this.isTrash.is)
                   .saveTheRecord()

            contact

        } else {

            this.isPersisted match {
                case true  => BeDoneSchema.contacts.update(this)
                case false => BeDoneSchema.contacts.insert(this)
            }

            this
        }
        

    }

}
