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
import net.liftweb.record.field.BooleanField
import net.liftweb.record.field.EnumField

import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.squerylrecord.RecordTypeMode._

import org.squeryl.annotations.Column

import java.io.StringReader
import java.io.StringWriter
import java.util.Calendar

object StuffType extends Enumeration 
{
    type StuffType = Value

    val Stuff    = Value(0, "Stuff")
    val Action   = Value(1, "Value")
    val Refrence = Value(2, "Reference")
    val Maybe    = Value(3, "Maybe")
}

object Stuff extends Stuff with MetaRecord[Stuff]
{
    def findByID(id: Int): Box[Stuff] = inTransaction {
        tryo { BeDoneSchema.stuffs.where(_.idField === id).single }
    }

    def findByUser(user: User): Box[List[Stuff]] = inTransaction {
        tryo {
            from(BeDoneSchema.stuffs)(table =>
                where(table.userID === user.idField and table.stuffType === StuffType.Stuff) 
                select(table)
                orderBy(table.createTime asc)
            ).toList
        }
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

    val isTrash = new BooleanField(this, false)
    val isStared = new BooleanField(this, false)

    val title = new StringField(this, "") {
        override def displayName = "標題"
        override def validations = valMinLen(1, "此為必填欄位")_ :: super.validations
    }
    val description = new TextareaField(this, 1000) {
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

    def topics = inTransaction(BeDoneSchema.stuffTopics.left(this).toList)
    def projects = inTransaction(BeDoneSchema.stuffProjects.left(this).toList)

    def descriptionHTML = {
        import org.tautua.markdownpapers.Markdown
        val reader = new StringReader(description.is)
        val writer = new StringWriter
        val markdown = new Markdown
        markdown.transform(reader, writer)

        val rawHTML = "<div>" + writer.toString + "</div>"
        scala.xml.XML.loadString(rawHTML)
    }

    override def saveTheRecord() = inTransaction(tryo{
        this.isPersisted match {
            case true  => BeDoneSchema.stuffs.update(this)
            case false => BeDoneSchema.stuffs.insert(this)
        }

        this
    })

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

    def removeProject(project: Project) = inTransaction {
        BeDoneSchema.stuffProjects.left(this).dissociate(project)
    }

    def addProject(project: Project) = inTransaction { 
        if (!project.isPersisted) { project.saveTheRecord() }
        BeDoneSchema.stuffProjects.left(this).associate(project)
    }

    def setProjects(projects: List[Project]) = inTransaction {
        val shouldRemove = this.projects.filterNot(projects.contains)
        val shouldAdd = projects.filterNot(this.projects.contains)

        shouldRemove.foreach(removeProject)
        shouldAdd.foreach(addProject)
    }

    def removeTopic(topic: Topic) = inTransaction {
        BeDoneSchema.stuffTopics.left(this).dissociate(topic)
    }

    def addTopic(topic: Topic) = inTransaction { 
        if (!topic.isPersisted) { topic.saveTheRecord() }
        BeDoneSchema.stuffTopics.left(this).associate(topic)
    }

    def setTopics(topics: List[Topic]) = inTransaction {
        val shouldRemove = this.topics.filterNot(topics.contains)
        val shouldAdd = topics.filterNot(this.topics.contains)

        shouldRemove.foreach(removeTopic)
        shouldAdd.foreach(addTopic)
    }

}


