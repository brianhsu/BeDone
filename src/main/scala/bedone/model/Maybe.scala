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

object Maybe extends Maybe with MetaRecord[Maybe]
{
    def findByUser(user: User): Box[List[Maybe]] = tryo {
        from(BeDoneSchema.stuffs, BeDoneSchema.maybes) ( (stuff, maybe) =>
            where(
                stuff.userID === user.idField and 
                stuff.idField === maybe.idField
            ) 
            select(maybe) orderBy(maybe.tickler.isNull, maybe.tickler)
        ).toList
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

