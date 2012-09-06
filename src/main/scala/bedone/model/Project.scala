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

object Project extends Project with MetaRecord[Project]
{
    import net.liftweb.common.Box._

    def findByID(id: Int): Box[Project] = 
        BeDoneSchema.projects.where(_.idField === id).headOption

    def findByUser(user: User): Box[List[Project]] = findByUser(user.idField.is)
    def findByUser(userID: Int): Box[List[Project]] = tryo{
        BeDoneSchema.projects.where(_.userID === userID).toList
    }

    def findByTitle(userID: Int, title: String): Box[Project] = 
        BeDoneSchema.projects.where(t => t.userID === userID and t.title === title).headOption

    def delete(project: Project) = {
        BeDoneSchema.projects.deleteWhere(p => p.idField === project.idField)
        BeDoneSchema.stuffProjects.deleteWhere(sp => sp.projectID === project.idField)
    }
}

class Project extends Record[Project] with KeyedRecord[Int]
{
    def meta = Project

    @Column(name="id")
    val idField = new IntField(this)
    val userID = new IntField(this)
    val title = new StringField(this, "")
    val description = new TextareaField(this, 1000)

    def allStuffs = BeDoneSchema.stuffProjects.right(this)
    def stuffs = allStuffs.filter(_.stuffType.is == StuffType.Stuff).toList
    def nextActions = allStuffs.filter(_.stuffType.is == StuffType.Action).toList
    def delegateds = allStuffs.filter(_.stuffType.is == StuffType.Delegated).toList
    def scheduleds = allStuffs.filter(_.stuffType.is == StuffType.Scheduled).toList
    def maybes = allStuffs.filter(_.stuffType.is == StuffType.Maybe).toList
    def references = allStuffs.filter(_.stuffType.is == StuffType.Reference).toList

    def className = "project%d%s" format (userID.is, hashHex(title.is))

    override def saveTheRecord() = tryo(BeDoneSchema.projects.insert(this))
}
