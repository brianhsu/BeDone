package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.common.Full

import net.liftweb.util.Helpers.tryo
import net.liftweb.util.Helpers.today

import net.liftweb.util.FieldError

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
import net.liftweb.record.field.StringField
import net.liftweb.record.field.TextareaField
import net.liftweb.record.field.DateTimeField
import net.liftweb.record.field.OptionalDateTimeField
import net.liftweb.record.field.OptionalLongField
import net.liftweb.record.field.BooleanField
import net.liftweb.record.field.EnumField

import net.liftweb.http.SHtml

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column

import java.io.StringReader
import java.io.StringWriter
import java.util.Calendar

import scala.xml.Text
import scala.xml.Elem

object StuffType extends Enumeration 
{
    type StuffType = Value

    val Stuff    = Value(0, "Stuff")
    val Action   = Value(1, "Value")
    val Delegated = Value(2, "Delegated")
    val Scheduled = Value(3, "Scheduled")
    val Reference = Value(4, "Reference")
    val Maybe    = Value(5, "Maybe")
}

object Trash 
{
    def findByUser(user: User): Box[List[Stuff]] = {
        tryo {
            from(BeDoneSchema.stuffs)(stuff =>
                where(
                    stuff.userID === user.idField and 
                    stuff.isTrash === true
                ) 
                select(stuff)
                orderBy(stuff.createTime asc)
            ).toList
        }
    }
}

object Stuff extends Stuff with MetaRecord[Stuff]
{
    import StuffType.StuffType

    def findByID(id: Int): Box[Stuff] = tryo { 
        BeDoneSchema.stuffs.where(_.idField === id).single 
    }

    def findByTopic(user: User, topicID: Int, 
                    stuffType: StuffType = StuffType.Stuff): Box[List[Stuff]] = 
    {
        import BeDoneSchema._

        tryo {
            from(stuffs, stuffTopics)((stuff, stuffTopic) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.stuffType === stuffType and
                    stuff.isTrash === false and
                    stuffTopic.stuffID === stuff.idField and
                    stuffTopic.topicID === topicID
                ) 
                select(stuff)
                orderBy(stuff.createTime asc)
            ).toList
        }
    }

    def findByProject(user: User, projectID: Int, 
                      stuffType: StuffType = StuffType.Stuff): Box[List[Stuff]] = 
    {
        import BeDoneSchema._

        tryo {
            from(stuffs, stuffProjects)((stuff, stuffProject) =>
                where(
                    stuff.userID === user.idField and 
                    stuff.stuffType === stuffType and
                    stuff.isTrash === false and
                    stuffProject.stuffID === stuff.idField and
                    stuffProject.projectID === projectID
                ) 
                select(stuff)
                orderBy(stuff.createTime asc)
            ).toList
        }
    }

    def findByUser(user: User, stuffType: StuffType = StuffType.Stuff): Box[List[Stuff]] = {
        tryo {
            from(BeDoneSchema.stuffs)(stuff =>
                where(
                    stuff.userID === user.idField and 
                    stuff.isTrash === false and
                    stuff.stuffType === stuffType
                ) 
                select(stuff)
                orderBy(stuff.createTime asc)
            ).toList
        }
    }

    def delete(stuff: Stuff) = {
        BeDoneSchema.stuffs.deleteWhere(s => s.idField === stuff.idField)
        BeDoneSchema.stuffProjects.deleteWhere(sp => sp.stuffID === stuff.idField)
        BeDoneSchema.stuffTopics.deleteWhere(st => st.stuffID === stuff.idField)
        BeDoneSchema.actions.deleteWhere(a => a.idField === stuff.idField)
        BeDoneSchema.maybes.deleteWhere(m => m.idField === stuff.idField)
        BeDoneSchema.scheduleds.deleteWhere(s => s.idField === stuff.idField)
        BeDoneSchema.delegateds.deleteWhere(d => d.idField === stuff.idField)
        BeDoneSchema.actionContexts.deleteWhere(ac => ac.actionID === stuff.idField)
    }

}

class Stuff extends Record[Stuff] with KeyedRecord[Int] 
{
    def meta = Stuff

    @Column(name="id")
    val idField = new IntField(this)
    val userID = new IntField(this)
    val createTime = new DateTimeField(this)
    val stuffType = new EnumField(this, StuffType, StuffType.Stuff)
    val gmailID = new OptionalLongField(this)

    val isTrash = new BooleanField(this, false)
    val isStared = new BooleanField(this, false)

    val title = new StringField(this, "") {
        override def displayName = "標題"
        override def validations = valMinLen(1, "此為必填欄位")_ :: super.validations
    }
    val description = new TextareaField(this, 10000) {
        override def displayName = "描述"
    }

    val deadline = new OptionalDateTimeField(this) {

        def afterToday(calendar: Option[Calendar]): List[FieldError] = {
            val error = FieldError(this, "完成期限要比今天晚")
            calendar.filter(_.before(today)).map(x => error).toList
        }

        override def displayName = "完成期限"
        override def helpAsHtml = Full(scala.xml.Text("格式為 yyyy-MM-dd"))
        override def validations = afterToday _ :: super.validations
    }

    def topics = BeDoneSchema.stuffTopics.left(this).toList
    def projects = BeDoneSchema.stuffProjects.left(this).toList

    def hasTopic(topicID: Int) = BeDoneSchema.stuffTopics.where { st => 
        st.stuffID === idField.is and st.topicID === topicID
    }.nonEmpty

    def hasProject(projectID: Int) = BeDoneSchema.stuffProjects.where { sp => 
        sp.stuffID === idField.is and sp.projectID === projectID
    }.nonEmpty

    def reInbox(title: Option[String] = None)  = {

        title.foreach(t => this.title(t))
        this.stuffType(StuffType.Stuff)

        BeDoneSchema.actions.deleteWhere(_.idField === this.idField)
        BeDoneSchema.delegateds.deleteWhere(_.idField === this.idField)
        BeDoneSchema.scheduleds.deleteWhere(_.idField === this.idField)
        BeDoneSchema.maybes.deleteWhere(_.idField === this.idField)

        this.saveTheRecord()
    }

    def descriptionHTML = {
        import org.tautua.markdownpapers.Markdown
        import scala.xml.XML

        if (gmailID.is == None) {
            val reader = new StringReader(description.is)
            val writer = new StringWriter
            val markdown = new Markdown
            markdown.transform(reader, writer)

            val rawHTML = "<div>" + writer.toString + "</div>"
            XML.loadString(rawHTML)

        } else {
            <pre>{description.is}</pre>
        }
    }

    override def saveTheRecord() = tryo{
        this.isPersisted match {
            case true  => BeDoneSchema.stuffs.update(this)
            case false => BeDoneSchema.stuffs.insert(this)
        }

        this
    }

    def addTopics(topicTitles: List[String]) {

        def createTopic(title: String) = {
            val topic = Topic.createRecord
            topic.userID(this.userID.is).title(title)
            topic.saveTheRecord()
            topic
        }

        def getTopic(title: String) = 
            Topic.findByTitle(userID.is, title).openOr(createTopic(title))

        topicTitles.map(getTopic).foreach(_.addStuff(this))
    }

    def addProjects(projectTitles: List[String]) {

        def createProject(title: String) = {
            val project = Project.createRecord
            project.userID(this.userID.is).title(title)
            project.saveTheRecord()
            project
        }

        def getProject(title: String) = 
            Project.findByTitle(userID.is, title).openOr(createProject(title))

        projectTitles.map(getProject).foreach(addProject)
    }

    def removeProject(project: Project) = 
        BeDoneSchema.stuffProjects.left(this).dissociate(project)

    def addProject(project: Project) = {
        if (!project.isPersisted) { project.saveTheRecord() }
        BeDoneSchema.stuffProjects.left(this).associate(project)
    }

    def setProjects(projects: List[Project]) = {
        val shouldRemove = this.projects.filterNot(projects.contains)
        val shouldAdd = projects.filterNot(this.projects.contains)

        shouldRemove.foreach(removeProject)
        shouldAdd.foreach(addProject)
    }

    def removeTopic(topic: Topic) = {
        BeDoneSchema.stuffTopics.left(this).dissociate(topic)
    }

    def addTopic(topic: Topic) = { 
        if (!topic.isPersisted) { topic.saveTheRecord() }
        BeDoneSchema.stuffTopics.left(this).associate(topic)
    }

    def setTopics(topics: List[Topic]) = {
        val shouldRemove = this.topics.filterNot(topics.contains)
        val shouldAdd = topics.filterNot(this.topics.contains)

        shouldRemove.foreach(removeTopic)
        shouldAdd.foreach(addTopic)
    }

    def gmailLink = gmailID.is.map { id => 
        "https://mail.google.com/mail/u/0/?shva=1#all/" + id.toHexString
    }

    def titleWithLink  = gmailLink match {
        case None       => Text(title.is)
        case Some(link) => <a href={link}>{title.is}</a>
    }

}


