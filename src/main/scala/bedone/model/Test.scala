package org.bedone.model

import net.liftweb.common.{Box, Full, Empty, Failure}
import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.util.FieldError

import net.liftweb.record.BaseField
import net.liftweb.record.field.LongField
import net.liftweb.record.field.StringField
import net.liftweb.record.field.PasswordField
import net.liftweb.record.field.OptionalEmailField
import net.liftweb.record.field.EmailField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._
import net.liftweb.common.Full

import org.squeryl.annotations.Column
import org.squeryl.Schema
import scala.xml.Text

object BeDoneSchema extends Schema {
    val users = table[User]("users")
}

object User extends User with MetaRecord[User]
{
    import BeDoneSchema._

    def findByUsername(username: String): Box[User] = inTransaction {
        users.where(_.username === username).toList match {
            case List(user) => Full(user)
            case Nil => Empty
            case _   => Failure("more than one user has username:" + username)
        }
    }

    def findByEmail(email: String): Box[User] = inTransaction {
        users.where(_.email === email).toList match {
            case List(user) => Full(user)
            case Nil => Empty
            case _   => Failure("more than one user has email:" + username)
        }
    }
}

trait MyValidation
{
    def isAlphaNumeric(field: BaseField)(value: String): List[FieldError] = {

        val isOK = value.forall { c =>
            (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9') || (c == '_')
        }

        isOK match {
            case true  => Nil
            case false => List(FieldError(field, "只能使用英文字母、數字和底線"))
        }
    }
}

class User extends Record[User] with KeyedRecord[Long] with MyValidation
{
    def meta = User

    @Column(name="id")
    val idField = new LongField(this, 1)

    val username = new StringField(this, "") {

        def alreadyTaken(username: String) = {
            User.findByUsername(username).isDefined match {
                case true  => List(FieldError(this, "已經有人註冊了這個使用者名稱"))
                case false => Nil
            }
        }

        override def displayName = "Username"
        override def validations = 
            valMinLen(1, "此為必填欄位") _ :: isAlphaNumeric(this)_ ::
            alreadyTaken _ :: super.validations
    }

    val email = new EmailField(this, 255) {

        def alreadyTaken(email: String) = {
            User.findByEmail(email).isDefined match {
                case true  => List(FieldError(this, "已經有人註冊過這個電子郵件了"))
                case false => Nil
            }
        }

        override def displayName = "EMail"
        override def validations = alreadyTaken _ :: super.validations
    }

    val password = new PasswordField(this) {
        override def displayName = "Password"
        override def helpAsHtml = Full(Text("至少需要七個字元"))
    }

    override def saveTheRecord(): Box[User] = inTransaction {
        try {
            BeDoneSchema.users.insert(this)
            Full(this)
        } catch {
            case e => new Failure("Error in insert to user table", Full(e), Empty)
        }
    }
}


