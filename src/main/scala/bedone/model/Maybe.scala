package org.bedone.model

import net.liftweb.common.Box

import net.liftweb.util.Helpers._
import net.liftweb.util.Helpers.today
import net.liftweb.util.FieldError

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.OptionalDateTimeField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column
import org.joda.time._

import java.util.Calendar
import java.util.Date

object Maybe extends Maybe with MetaRecord[Maybe]
{
    def findByTopic(user: User, topicID: Int): Box[List[Maybe]] = tryo {
        import BeDoneSchema._

        from(stuffs, maybes, stuffTopics) ( (stuff, maybe, stuffTopic) =>
            where(
                stuff.userID === user.idField and 
                stuff.idField === maybe.idField and
                stuffTopic.stuffID === stuff.idField and
                stuffTopic.topicID === topicID
            ) 
            select(maybe) orderBy(maybe.tickler.isNull, maybe.tickler)
        ).toList
    }

    def findByProject(user: User, projectID: Int): Box[List[Maybe]] = tryo {
        import BeDoneSchema._

        from(stuffs, maybes, stuffProjects) ( (stuff, maybe, stuffProject) =>
            where(
                stuff.userID === user.idField and 
                stuff.idField === maybe.idField and
                stuffProject.stuffID === stuff.idField and
                stuffProject.projectID === projectID
            ) 
            select(maybe) orderBy(maybe.tickler.isNull, maybe.tickler)
        ).toList
    }

    def findByUser(user: User): Box[List[Maybe]] = tryo {
        from(BeDoneSchema.stuffs, BeDoneSchema.maybes) ( (stuff, maybe) =>
            where(
                stuff.userID === user.idField and 
                stuff.idField === maybe.idField
            ) 
            select(maybe) orderBy(maybe.tickler.isNull, maybe.tickler)
        ).toList
    }

    def outdated(user: User) = tryo {
        val maybes = 
            from(BeDoneSchema.stuffs, BeDoneSchema.maybes) ( (stuff, maybe) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.idField === maybe.idField and
                    maybe.tickler.isNotNull
                ) 
                select(maybe) orderBy(maybe.tickler)
            ).toList
        
        maybes.filter(_.tickler.is.get.getTimeInMillis < now.getTime)
    }

}

class Maybe extends Record[Maybe] with KeyedRecord[Int]
{
    def meta = Maybe

    @Column(name="stuffID")
    val idField = new IntField(this)
    val tickler = new OptionalDateTimeField(this) {

        def afterToday(calendar: Option[Calendar]): List[FieldError] = {
            val error = FieldError(this, "提醒時間要比今天晚")
            val tomorrow = ((new DateMidnight) plus (1 day)).toDate

            calendar.filter(_.getTime.before(tomorrow)).map(x => error).toList
        }

        override def validations = afterToday _ :: super.validations
    }

    def stuff = Stuff.findByID(idField.is).get

    override def saveTheRecord() = tryo{
        this.isPersisted match {
            case true  => BeDoneSchema.maybes.update(this)
            case false => BeDoneSchema.maybes.insert(this)
        }

        this
    }

}

