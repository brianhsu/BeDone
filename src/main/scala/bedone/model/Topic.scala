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

object Topic extends Topic with MetaRecord[Topic]
{
    import net.liftweb.common.Box._

    def findByID(id: Int): Box[Topic] = 
        BeDoneSchema.topics.where(_.idField === id).headOption

    def findByUser(user: User): Box[List[Topic]] = findByUser(user.idField.is)
    def findByUser(userID: Int): Box[List[Topic]] = tryo {
        from(BeDoneSchema.topics) { topic =>
            where(topic.userID === userID).
            select(topic).
            orderBy(topic.title)
        }.toList
    }

    def findByTitle(userID: Int, title: String): Box[Topic] = 
        BeDoneSchema.topics.where(t => t.userID === userID and t.title === title).headOption

    def delete(topic: Topic) = {
        BeDoneSchema.topics.deleteWhere(t => t.idField === topic.idField)
        BeDoneSchema.stuffTopics.deleteWhere(st => st.topicID === topic.idField)
    }

    def paramParser(param: String): Box[Topic] = tryo {
        inTransaction {
            CurrentUser.is.flatMap { user => 
                findByID(param.toInt).filter(_.userID.is == user.idField.is)
            }.get
        }
    }

}

class Topic extends Record[Topic] with KeyedRecord[Int]
{
    def meta = Topic

    @Column(name="id")
    val idField = new IntField(this)
    val userID = new IntField(this)
    val title = new StringField(this, "")
    val description = new TextareaField(this, 1000)

    override def saveTheRecord() = tryo {
        this.isPersisted match {
            case true  => BeDoneSchema.topics.update(this)
            case false => BeDoneSchema.topics.insert(this)
        }

        this
    }

    def className = "topic%d%s" format (userID.is, hashHex(title.is))

    def addStuff(stuff: Stuff) = BeDoneSchema.stuffTopics.right(this).associate(stuff)

    def allStuffs = BeDoneSchema.stuffTopics.right(this)

    def stuffs = allStuffs.filter { x =>
        x.stuffType.is == StuffType.Stuff && !x.isTrash.is
    }.toList

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

}
