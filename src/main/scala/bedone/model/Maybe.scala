package org.bedone.model

import net.liftweb.common.Box

import net.liftweb.http.S

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

case class MaybeT(stuff: Stuff, maybe: Maybe)

object Maybe extends Maybe with MetaRecord[Maybe]
{
    def toMaybeT(t: (Stuff, Maybe)) = MaybeT(t._1, t._2)

    def findByTopic(user: User, topicID: Int): Box[List[MaybeT]] = tryo {
        import BeDoneSchema._

        from(stuffs, maybes, stuffTopics) ( (stuff, maybe, stuffTopic) =>
            where(
                stuff.userID === user.idField and 
                stuff.idField === maybe.idField and
                stuff.isTrash === false and
                stuffTopic.stuffID === stuff.idField and
                stuffTopic.topicID === topicID
            ) 
            select(stuff, maybe) orderBy(maybe.tickler.isNull, maybe.tickler)
        ).map(toMaybeT).toList
    }

    def findByProject(user: User, projectID: Int): Box[List[MaybeT]] = tryo {
        import BeDoneSchema._

        from(stuffs, maybes, stuffProjects) ( (stuff, maybe, stuffProject) =>
            where(
                stuff.userID === user.idField and 
                stuff.idField === maybe.idField and
                stuff.isTrash === false and
                stuffProject.stuffID === stuff.idField and
                stuffProject.projectID === projectID
            ) 
            select(stuff, maybe) orderBy(maybe.tickler.isNull, maybe.tickler)
        ).map(toMaybeT).toList
    }

    def findByUser(user: User): Box[List[MaybeT]] = tryo {
        from(BeDoneSchema.stuffs, BeDoneSchema.maybes) ( (stuff, maybe) =>
            where(
                stuff.userID === user.idField and 
                stuff.isTrash === false and
                stuff.idField === maybe.idField
            ) 
            select(stuff, maybe) orderBy(maybe.tickler.isNull, maybe.tickler)
        ).map(toMaybeT).toList
    }

    def outdated(user: User) = tryo {
        val maybes = 
            from(BeDoneSchema.stuffs, BeDoneSchema.maybes) ( (stuff, maybe) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.idField === maybe.idField and
                    stuff.isTrash === false and
                    maybe.tickler.isNotNull
                ) 
                select(stuff, maybe) orderBy(maybe.tickler)
            ).map(toMaybeT).toList
        
        maybes.filter(_.maybe.tickler.is.get.getTimeInMillis < now.getTime)
    }

}

class Maybe extends Record[Maybe] with KeyedRecord[Int]
{
    def meta = Maybe

    @Column(name="stuffID")
    val idField = new IntField(this)
    val tickler = new OptionalDateTimeField(this) {

        def afterToday(calendar: Option[Calendar]): List[FieldError] = {
            val error = FieldError(this, S.?("Tickler must later than today."))
            val tomorrow = ((new DateMidnight) plus (1 day)).toDate

            calendar.filter(_.getTime.before(tomorrow)).map(x => error).toList
        }

        override def validations = afterToday _ :: super.validations
    }

    override def saveTheRecord() = tryo{
        this.isPersisted match {
            case true  => BeDoneSchema.maybes.update(this)
            case false => BeDoneSchema.maybes.insert(this)
        }

        this
    }

}

