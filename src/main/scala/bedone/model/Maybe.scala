package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.util.Helpers.tryo

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.OptionalDateTimeField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column

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
    val tickler = new OptionalDateTimeField(this)

    def stuff = Stuff.findByID(idField.is).get

}

