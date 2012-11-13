package org.bedone.model

import org.bedone.lib._

import org.squeryl.annotations.Column

import net.liftweb.common.{Box, Full, Empty, Failure}

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field._

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import net.liftweb.http.SessionVar
import net.liftweb.http.S

import net.liftweb.util.Helpers.tryo
import net.liftweb.util.FieldError
import net.liftweb.util.Helpers._
import net.liftweb.util.Mailer
import net.liftweb.util.Mailer._

import scala.xml.Text

import java.util.Calendar

object CurrentUser extends SessionVar[Box[User]](Empty)
object ActivationStatus extends Enumeration("Done", "Register", "Reset")
{
    type Status = Value
    val Done, Register, Reset = Value
}

object User extends User with MetaRecord[User]
{
    import BeDoneSchema._

    def findByUsername(username: String): Box[User] = {
        users.where(_.username === username).toList match {
            case List(user) => Full(user)
            case Nil => Empty
            case _   => Failure("more than one user has username:" + username)
        }
    }

    def findByEmail(email: String): Box[User] = {
        users.where(_.email === email).toList match {
            case List(user) => Full(user)
            case Nil => Empty
            case _   => Failure("more than one user has email:" + username)
        }
    }

    def isLoggedIn = CurrentUser.get.isDefined
}

class User extends Record[User] with KeyedRecord[Int] with MyValidation
{
    def meta = User

    @Column(name="id")
    val idField = new IntField(this)

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

    val activationStatus = new EnumField(this, ActivationStatus, ActivationStatus.Register)
    val activationCode = new OptionalStringField(this, 40)
    val activationDue = new OptionalDateTimeField(this)

    override def saveTheRecord() = tryo {
        this.isPersisted match {
            case true  => BeDoneSchema.users.update(this)
            case false => BeDoneSchema.users.insert(this)
        }

        this
    }

    def activate()
    {
        this.activationCode(None)
        this.activationStatus(ActivationStatus.Done)
    }

    def sendActivationCode()
    {
        val confirmURL = "%s/confirmEMail?username=%s&code=%s".format(S.hostAndPath, username.is, activationCode.is.getOrElse(""))
        val subject = Subject("[BeDone] 帳號註冊確認信")
        val body = PlainMailBodyType("""
            |%s 您好，
            |
            |感謝您在 BeDone 上註冊帳號，請使用下列的網址正式啟用：
            |
            |%s
            |
            |若您從未在 BeDone 上註冊過帳號，請直接勿略此信即可。
            |
            |謝謝！祝您使用愉快！
        """.format(username.is, confirmURL).stripMargin)

        Mailer.sendMail(From("brianhsu.hsu@gmail.com"), subject, To(email.is), body)
    }

    def sendResetPassword()
    {
        val confirmURL = "%s/resetPassword?username=%s&code=%s".format(S.hostAndPath, username.is, activationCode.is.getOrElse(""))
        val subject = Subject("[BeDone] 重設密碼連結")
        val body = PlainMailBodyType("""
            |%s 您好，
            |
            |聽說您忘記自己的密碼了，您可以用以下的連結重新設定密碼：
            |
            |%s
            |
            |若您從未在 BeDone 上註冊過帳號，請直接勿略此信即可。
            |
            |謝謝！祝您使用愉快！
        """.format(username.is, confirmURL).stripMargin)

        Mailer.sendMail(From("brianhsu.hsu@gmail.com"), subject, To(email.is), body)
    }

    def resetActivationCode(status: ActivationStatus.Value)
    {
        val dueDate = Calendar.getInstance
        dueDate.setTime((now:TimeSpan) + hours(24))

        this.activationStatus(status)
        this.activationCode(randomString(40))
        this.activationDue(dueDate)

        status match {
            case ActivationStatus.Register => sendActivationCode()
            case ActivationStatus.Reset => sendResetPassword()
            case ActivationStatus.Done =>
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

    def avatarURL = Gravatar.avatarURL(email.is)
}
