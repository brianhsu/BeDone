package org.bedone.lib

import org.bedone.model.Stuff
import org.bedone.model.User
import org.bedone.model.GMailPreference

import net.liftweb.http.S
import net.liftweb.squerylrecord.RecordTypeMode._

import com.google.code.com.sun.mail.imap.IMAPFolder
import com.google.code.com.sun.mail.imap.IMAPMessage

import com.google.code.javax.mail.Session
import com.google.code.javax.mail.Flags.Flag
import com.google.code.javax.mail.FetchProfile
import com.google.code.javax.mail.Folder
import com.google.code.javax.mail.Message
import com.google.code.javax.mail.Part
import com.google.code.javax.mail.Multipart
import javax.mail.internet.MimeUtility

object GMailFetcher
{
    def apply(user: User): Option[GMailFetcher] = inTransaction {

        GMailPreference.findByUser(user).filter(_.usingGMail.is).map { setting =>
            val plainPassword = PasswordHelper.decrypt(setting.password.is).get
            new GMailFetcher(user.idField.is, setting.username.is, plainPassword)
        }.toOption

    }
}

class GMailFetcher(userID: Int, username: String, password: String)
{
    private val BeDoneFolderName = "BeDone"

    private lazy val store = {
        val props = System.getProperties();
        props.setProperty("mail.store.protocol", "imaps");
        val session = Session.getDefaultInstance(props, null);
        session.getStore("imaps");
    }

    private lazy val beDoneFolder = 
    {
        val defaultFolder = store.getDefaultFolder
        val folder = defaultFolder.getFolder(BeDoneFolderName)

        if (!folder.exists) {
            folder.create(Folder.HOLDS_MESSAGES)
        }

        folder
    }

    def validate: Option[Throwable] = {

        def convertException(error: Throwable) = {
            val errorMessage = error.getMessage
            val reason = if (errorMessage.contains("Invalid credentials")) {
                S.?("Username or password is invalid, please check your setting.")
            } else if (errorMessage.contains("Your account is not enabled for IMAP use.")) {
                S.?("GMail IMAP support is not enabled.")
            } else {
                errorMessage
            }

            new Exception(reason)
        }

        try {
            store.connect("imap.gmail.com", username, password)
            store.close()
            None
        } catch {
            case e: Exception => Some(convertException(e))
        }
    }

    def getUntouchedMail() =
    {
        def inBeDone(message: IMAPMessage) = {
            val labels = Option(message.getGoogleMessageLabels).map(_.toList).getOrElse(Nil)
            labels.contains(BeDoneFolderName)
        }

        val inbox = store.getFolder("INBOX")

        inbox.open(Folder.READ_ONLY)

        val profiler = new FetchProfile()
        val messages = inbox.getMessages()

        profiler.add(IMAPFolder.FetchProfileItem.X_GM_MSGID)
        profiler.add(IMAPFolder.FetchProfileItem.X_GM_LABELS)

        inbox.fetch(messages, profiler);

        messages.map(_.asInstanceOf[IMAPMessage]).filterNot(inBeDone).toList
    }

    def addBeDoneLabel(messages: List[Message]) =
    {
        beDoneFolder.appendMessages(messages.toArray)
    }

    def decodeSubject(subject: String) = {
        subject.contains("=?") match {
            case true  => 
                MimeUtility.decodeText(subject.replaceAll("(?i)\\=\\?GB2312", "=?GBK"))
            case false => 
                new String(subject.getBytes("iso8859-1"), "utf-8")
        }
    }

    def createStuff(message: IMAPMessage) = 
    {
        val title = message.getHeader("subject").map(decodeSubject)
                           .headOption.getOrElse(S.?("No Title"))

        val gmailID = message.getGoogleMessageId
        val desc = getBodyText(message).map(_.take(10000))

        val stuff = Stuff.createRecord.userID(userID)
                         .title(title).gmailID(gmailID)
                         .description(desc)

        stuff
    }

    def getMultiPartText(multipart: Multipart): Option[String] =
    {
        val allParts = (0 until multipart.getCount).map(multipart.getBodyPart)
        val plainText = allParts.filter(_.isMimeType("text/plain")).headOption
        val htmlText = allParts.filter(_.isMimeType("text/html")).headOption

        (plainText orElse htmlText).map(_.getContent.toString)
    }

    def getBodyText(part: Part): Option[String] = 
    {
        val mimeType = part.getContentType.toLowerCase

        mimeType match {
            case t if t.startsWith("text/plain") => 
                Some(part.getContent.toString)

            case m if m.startsWith("multipart/alternative") => 
                getMultiPartText(part.getContent.asInstanceOf[Multipart])

            case m if m.startsWith("multipart/mixed") =>
                val multipart = part.getContent.asInstanceOf[Multipart]
                val allParts = (0 until multipart.getCount).map(multipart.getBodyPart)
                allParts.flatMap(getBodyText).headOption

            case o => Some("No plain-text message.")
        }
    }

    def sync() =
    {
        if (!store.isConnected) {
            store.connect("imap.gmail.com", username, password)
        }

        val untouchedMail = getUntouchedMail
        val stuffs = untouchedMail.map(createStuff)

        addBeDoneLabel(untouchedMail)

        transaction { 
            stuffs.map(_.saveTheRecord())
        }

        store.close()
        stuffs
    }
}
