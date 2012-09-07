package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.common.Full

import net.liftweb.util.Helpers.tryo
import net.liftweb.util.Helpers.hashHex

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

object Context extends Context with MetaRecord[Context]
{
    def findByID(id: Int): Box[Context] = 
        BeDoneSchema.contexts.where(_.idField === id).headOption

    def findByUser(user: User): Box[List[Context]] = findByUser(user.idField.is)
    def findByUser(userID: Int): Box[List[Context]] = tryo{
        from(BeDoneSchema.contexts)(c => 
            where(c.userID === userID)
            select(c)
            orderBy(c.idField)
        ).toList
    }

    def findByTitle(userID: Int, title: String): Box[Context] = 
        BeDoneSchema.contexts.where(t => t.userID === userID and t.title === title).headOption

    def delete(context: Context) = {
        BeDoneSchema.contexts.deleteWhere(c => c.idField === context.idField)
        BeDoneSchema.actionContexts.deleteWhere(ac => ac.contextID === context.idField)
    }

}

class Context extends Record[Context] with KeyedRecord[Int]
{
    def meta = Context

    @Column(name="id")
    val idField = new IntField(this)
    val userID = new IntField(this)
    val title = new StringField(this, "")
    val description = new TextareaField(this, 1000)

    def className = "context%d%s" format (userID.is, hashHex(title.is))

    override def saveTheRecord() = tryo(BeDoneSchema.contexts.insert(this))
}
