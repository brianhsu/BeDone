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

object GMailPreference extends GMailPreference with MetaRecord[GMailPreference]
{
    import net.liftweb.common.Box._

    def findByUser(user: User): Box[GMailPreference] = findByUser(user.idField.is)
    def findByUser(userID: Int): Box[GMailPreference] = tryo {
        BeDoneSchema.gmailPreference.where(_.idField === userID).single
    }
}

class GMailPreference extends Record[GMailPreference] with KeyedRecord[Int]
{
    def meta = GMailPreference

    @Column(name="userID")
    val idField = new IntField(this)

    val username = new StringField(this, "")
    val password = new StringField(this, "")
    val usingGMail = new BooleanField(this, false)

    override def saveTheRecord() = tryo {
        this.isPersisted match {
            case true  => BeDoneSchema.gmailPreference.update(this)
            case false => BeDoneSchema.gmailPreference.insert(this)
        }

        this
    }

}
