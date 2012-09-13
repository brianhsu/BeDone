package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.util.Helpers.tryo

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.BooleanField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column

object Delegated extends Delegated with MetaRecord[Delegated]
{

    def findByTopic(user: User, topicID: Int): Box[List[Delegated]] = tryo {
        import BeDoneSchema._

        from(stuffs, delegateds, stuffTopics) ( (stuff, delegated, stuffTopic) =>
            where(
                stuff.userID === user.idField.is and 
                stuff.stuffType === StuffType.Delegated and
                stuff.idField === delegated.idField.is and
                stuff.isTrash === false and
                stuffTopic.stuffID === stuff.idField and
                stuffTopic.topicID === topicID
            ) 
            select(delegated) 
            orderBy(stuff.createTime)
        ).toList
    }

    def findByProject(user: User, projectID: Int): Box[List[Delegated]] = tryo {
        import BeDoneSchema._

        from(stuffs, delegateds, stuffProjects) ( (stuff, delegated, stuffProject) =>
            where(
                stuff.userID === user.idField.is and 
                stuff.stuffType === StuffType.Delegated and
                stuff.idField === delegated.idField.is and
                stuff.isTrash === false and
                stuffProject.stuffID === stuff.idField and
                stuffProject.projectID === projectID
            ) 
            select(delegated) 
            orderBy(stuff.createTime)
        ).toList
    }

    def findByUser(user: User): Box[List[Delegated]] = tryo {
        from(BeDoneSchema.stuffs, BeDoneSchema.delegateds) ( (stuff, delegated) =>
            where(
                stuff.userID === user.idField.is and 
                stuff.stuffType === StuffType.Delegated and
                stuff.isTrash === false and
                stuff.idField === delegated.idField.is
            ) 
            select(delegated) 
            orderBy(stuff.createTime)
        ).toList
    }

}

class Delegated extends Record[Delegated] with KeyedRecord[Int]
{
    def meta = Delegated

    @Column(name="actionID")
    val idField = new IntField(this)
    val contactID = new IntField(this)

    val hasInformed = new BooleanField(this, false)

    def action = Action.findByID(idField.is).get
    def contact = Contact.findByID(contactID.is).get

    override def saveTheRecord() = tryo {
        this.isPersisted match {
            case true  => BeDoneSchema.delegateds.update(this)
            case false => BeDoneSchema.delegateds.insert(this)
        }

        this
    }

}


