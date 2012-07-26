package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.common.Full

import net.liftweb.util.Helpers.tryo

import net.liftweb.util.FieldError

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.LongField
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

object StuffProject extends StuffProject with MetaRecord[StuffProject]
class StuffProject extends Record[StuffProject] 
{
    def meta = StuffProject

    val stuffID = new LongField(this)
    val projectID = new LongField(this)

    def project = Project.findByID(projectID.is).open_!
    def stuff = Stuff.findByID(stuffID.is).open_!

    override def saveTheRecord = inTransaction ( tryo {
        import BeDoneSchema.stuffProjects

        val oldProjects = stuffProjects.where { t => 
            t.stuffID === stuffID and t.projectID === projectID
        }

        oldProjects.toList match {
            case Nil => stuffProjects.insert(this)
            case xs  => this
        }
    })

}

