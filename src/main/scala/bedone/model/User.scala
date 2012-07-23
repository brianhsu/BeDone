package org.bedone.model

import net.liftweb.common.{Box, Full, Empty, Failure}

import net.liftweb.util.FieldError

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.LongField
import net.liftweb.record.field.StringField
import net.liftweb.record.field.PasswordField
import net.liftweb.record.field.EmailField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column
import scala.xml.Text

import net.liftweb.http.SessionVar
import net.liftweb.http.S

object CurrentUser extends SessionVar[Box[User]](Empty)

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

    def isLoggedIn = CurrentUser.get.isDefined
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

    def logout(postAction: => Any = ()) {
        CurrentUser.set(Empty)
        postAction
        S.session.foreach(_.destroySession)
    }

    def login(postAction: => Any = ()) {
        CurrentUser.set(Full(this))
        postAction
    }
}
