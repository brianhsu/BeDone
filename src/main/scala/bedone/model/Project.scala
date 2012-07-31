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

object Project extends Project with MetaRecord[Project]
{
    import net.liftweb.common.Box._

    def findByID(id: Int): Box[Project] = inTransaction (
        BeDoneSchema.projects.where(_.idField === id).headOption
    )

    def findByUser(user: User): Box[List[Project]] = findByUser(user.idField.is)
    def findByUser(userID: Int): Box[List[Project]] = inTransaction(tryo{
        BeDoneSchema.projects.where(_.userID === userID).toList
    })

    def findByTitle(userID: Int, title: String): Box[Project] = inTransaction(
        BeDoneSchema.projects.where(t => t.userID === userID and t.title === title).headOption
    )
}

class Project extends Record[Project] with KeyedRecord[Int]
{
    def meta = Project

    @Column(name="id")
    val idField = new IntField(this)
    val userID = new IntField(this)
    val title = new StringField(this, "")
    val description = new TextareaField(this, 1000)

    def stuffs = inTransaction {
        BeDoneSchema.stuffProjects.right(this)
                    .filter(_.stuffType.is == StuffType.Stuff).toList
    }

    override def saveTheRecord() = inTransaction { tryo(BeDoneSchema.projects.insert(this)) }
}
