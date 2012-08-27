package org.bedone.lib

import org.bedone.model.Stuff
import org.bedone.model.User

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

object GMailFetcher
{
    def apply(user: User): Option[GMailFetcher] = {
        None
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
        try {
            store.connect("imap.gmail.com", username, password)
            store.close()
            None
        } catch {
            case e => Some(e)
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

    def createStuff(message: IMAPMessage) = 
    {
        val title = message.getSubject
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

            case o => None

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
