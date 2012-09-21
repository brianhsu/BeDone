package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.util.Helpers._
import net.liftweb.util.FieldError

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.OptionalIntField
import net.liftweb.record.field.OptionalStringField
import net.liftweb.record.field.OptionalDateTimeField
import net.liftweb.record.field.DateTimeField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column

import java.util.Calendar
import scala.xml.Text

case class ScheduledT(stuff: Stuff, action: Action, scheduled: Scheduled)

object Scheduled extends Scheduled with MetaRecord[Scheduled]
{
    def toScheduledT(t: (Stuff, Action, Scheduled)) = ScheduledT(t._1, t._2, t._3)
    def findByTopic(user: User, topicID: Int): Box[List[ScheduledT]] = {
        import BeDoneSchema._

        tryo {
            from(stuffs, actions, scheduleds, stuffTopics) ( (stuff, action, scheduled, st) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.stuffType === StuffType.Scheduled and
                    stuff.idField === scheduled.idField and
                    stuff.isTrash === false and
                    action.idField === stuff.idField and
                    st.stuffID === stuff.idField and
                    st.topicID === topicID
                ) 
                select(stuff, action, scheduled) 
                orderBy(scheduled.startTime)
            ).map(toScheduledT).toList
        }
    }

    def findByProject(user: User, projectID: Int): Box[List[ScheduledT]] = {
        import BeDoneSchema._

        tryo {
            from(stuffs, actions, scheduleds, stuffProjects) ( (stuff, action, scheduled, sp) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.stuffType === StuffType.Scheduled and
                    stuff.idField === scheduled.idField and
                    stuff.isTrash === false and
                    action.idField === stuff.idField and
                    sp.stuffID === stuff.idField and
                    sp.projectID === projectID
                ) 
                select(stuff, action, scheduled) 
                orderBy(scheduled.startTime)
            ).map(toScheduledT).toList
        }
    }

    def findByUser(user: User): Box[List[ScheduledT]] = {
        import BeDoneSchema._

        tryo {
            from(stuffs, actions, scheduleds) ( (stuff, action, scheduled) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.stuffType === StuffType.Scheduled and
                    stuff.isTrash === false and
                    action.idField === stuff.idField and
                    stuff.idField === scheduled.idField
                ) 
                select(stuff, action, scheduled) 
                orderBy(scheduled.startTime)
            ).map(toScheduledT).toList
        }
    }
}

class Scheduled extends Record[Scheduled] with KeyedRecord[Int]
{
    def meta = Scheduled

    @Column(name="actionID")
    val idField = new IntField(this)
    val startTime = new DateTimeField(this)

    val endTime = new OptionalDateTimeField(this) {

        def isAfterStartTime(endTime: Option[Calendar]): List[FieldError] = 
        {
            endTime.forall(x => x.getTime.getTime > startTime.is.getTime.getTime) match {
                case true  => Nil
                case false => new FieldError(this, Text("結束時間必須在開始時間後")) :: Nil
            }
        }

        override def validations = isAfterStartTime _ :: super.validations
    }

    val location = new OptionalStringField(this, 255)

    def action = Action.findByID(idField.is).get


    override def saveTheRecord() = tryo{
        this.isPersisted match {
            case true  => BeDoneSchema.scheduleds.update(this)
            case false => BeDoneSchema.scheduleds.insert(this)
        }

        this
    }
}


