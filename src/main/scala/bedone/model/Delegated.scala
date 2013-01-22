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

case class DelegatedT(stuff: Stuff, action: Action, delegated: Delegated)

object Delegated extends Delegated with MetaRecord[Delegated]
{
    def toDelegatedT(t: (Stuff, Action, Delegated)) = DelegatedT(t._1, t._2, t._3)

    def findByTopic(user: User, topicID: Int): Box[List[DelegatedT]] = tryo {
        import BeDoneSchema._

        from(stuffs, actions, delegateds, stuffTopics) ( (stuff, action, delegated, st) =>
            where(
                stuff.userID === user.idField.is and 
                stuff.stuffType === StuffType.Delegated and
                stuff.idField === delegated.idField.is and
                stuff.isTrash === false and
                action.idField === stuff.idField and
                st.stuffID === stuff.idField and
                st.topicID === topicID
            ) 
            select(stuff, action, delegated) 
            orderBy(stuff.createTime)
        ).map(toDelegatedT).toList
    }

    def findByProject(user: User, projectID: Int): Box[List[DelegatedT]] = tryo {
        import BeDoneSchema._

        from(stuffs, actions, delegateds, stuffProjects) ( (stuff, action, delegated, sp) =>
            where(
                stuff.userID === user.idField.is and 
                stuff.stuffType === StuffType.Delegated and
                stuff.idField === delegated.idField.is and
                stuff.isTrash === false and
                action.idField === stuff.idField and
                sp.stuffID === stuff.idField and
                sp.projectID === projectID
            ) 
            select(stuff, action, delegated) 
            orderBy(stuff.createTime)
        ).map(toDelegatedT).toList
    }

    def findByUser(user: User): Box[List[DelegatedT]] = tryo {

        import BeDoneSchema._

        from(stuffs, actions, delegateds) ( (stuff, action, delegated) =>
            where(
                stuff.userID === user.idField.is and 
                stuff.stuffType === StuffType.Delegated and
                stuff.isTrash === false and
                action.idField === stuff.idField and
                stuff.idField === delegated.idField.is
            ) 
            select(stuff, action, delegated) 
            orderBy(stuff.createTime)
        ).map(toDelegatedT).toList
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


