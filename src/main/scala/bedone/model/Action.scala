package org.bedone.model

import net.liftweb.common.{Box, Full, Empty, Failure}

import net.liftweb.util.FieldError

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.BooleanField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column
import scala.xml.Text

import net.liftweb.http.SessionVar
import net.liftweb.http.S
import net.liftweb.util.Helpers.tryo

object Action extends Action with MetaRecord[Action]
class Action extends Record[Action]
{
    def meta = Action

    val stuffID = new IntField(this)
    val isDone = new BooleanField(this, false)
}


