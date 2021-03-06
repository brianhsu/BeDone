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
        from(BeDoneSchema.projects) { project => 
            where(project.userID === userID).
            select(project).
            orderBy(project.title)
        }.toList
    }

    def findByTitle(userID: Int, title: String): Box[Project] = 
        BeDoneSchema.projects.where(t => t.userID === userID and t.title === title).headOption

    def delete(project: Project) = {
        BeDoneSchema.projects.deleteWhere(p => p.idField === project.idField)
        BeDoneSchema.stuffProjects.deleteWhere(sp => sp.projectID === project.idField)
    }

    def paramParser(param: String): Box[Project] = tryo {
        inTransaction {
            CurrentUser.is.flatMap { user => 
                findByID(param.toInt).filter(_.userID.is == user.idField.is)
            }.get
        }
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
    def nextActions = allStuffs.filter { x => 
        x.stuffType.is == StuffType.Action && !x.isTrash.is 
    }.toList

    def delegateds = allStuffs.filter { x => 
        x.stuffType.is == StuffType.Delegated && !x.isTrash.is
    }.toList

    def scheduleds = allStuffs.filter { x => 
        x.stuffType.is == StuffType.Scheduled && !x.isTrash.is
    }.toList

    def maybes = allStuffs.filter { x => 
        x.stuffType.is == StuffType.Maybe && !x.isTrash.is
    }.toList

    def references = allStuffs.filter { x => 
        x.stuffType.is == StuffType.Reference && !x.isTrash.is
    }.toList

    def className = "project%d%s" format (userID.is, hashHex(title.is))

    override def saveTheRecord() = tryo {
        this.isPersisted match {
            case true  => BeDoneSchema.projects.update(this)
            case false => BeDoneSchema.projects.insert(this)
        }

        this
    }

}
