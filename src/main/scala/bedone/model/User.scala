package org.bedone.model

import org.bedone.lib._

import org.squeryl.annotations.Column

import net.liftweb.actor.LiftActor
import net.liftweb.common._
import net.liftweb.record._
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
import net.liftweb.util.Schedule

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
                case true  => List(FieldError(this, S.?("This username is already taken.")))
                case false => Nil
            }
        }

        override def displayName = S.?("Username")
        override def validations = 
            valMinLen(1, S.?("This field is required.")) _ :: isAlphaNumeric(this)_ ::
            alreadyTaken _ :: super.validations
    }

    val email = new EmailField(this, 255) {

        def alreadyTaken(email: String) = {
            User.findByEmail(email).isDefined match {
                case true  => List(FieldError(this, S.?("This EMail is already registered.")))
                case false => Nil
            }
        }

        override def displayName = S.?("EMail")
        override def validations = alreadyTaken _ :: super.validations
    }

    val password = new PasswordField(this) {
        override def displayName = S.?("Password")
        override def helpAsHtml = Full(Text(S.?("Password need at least 7 characters")))
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
        val subject = Subject(S.?("[BeDone] Sign-up confirmation"))

        val confirmURL = {
            "%s/account/confirmEMail?username=%s&code=%s".format(
                S.hostAndPath, username.is, activationCode.is.getOrElse("")
            )
        }

        val message = S.loc("ConfirmEMailBody").map(_.text).openOr("""
            |Hello %s,
            |
            |Thanks for your registeration, please use the following URL
            |link to active your BeDone account:
            |
            |%s
            |
            |If you never sing-up on BeDone, you could simply ignore this
            |message.
            |
            |Tanks, have a nice day!
        """.format(username.is, confirmURL).stripMargin)

        val body = PlainMailBodyType(message)

        Mailer.sendMail(From("brianhsu.hsu@gmail.com"), subject, To(email.is), body)
    }

    def sendResetPassword()
    {
        val subject = Subject(S.?("[BeDone] Password reset URL"))

        val confirmURL = {
            "%s/account/resetPassword?username=%s&code=%s".format(
                S.hostAndPath, username.is, activationCode.is.getOrElse("")
            )
        }

        val message = S.loc("ResetPasswordEMailBody").map(_.text).openOr("""
            |Hello %s,
            |
            |Lost your password? Don't worry, you could use the following link
            |to reset your BeDone password:
            |
            |%s
            |
            |If you never sing-up on BeDone, you could simply ignore this
            |message.
            |
            |Tanks, have a nice day!
        """.format(username.is, confirmURL).stripMargin)

        val body = PlainMailBodyType(message)

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
