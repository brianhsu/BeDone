package org.bedone.model

import net.liftweb.common.Box
import net.liftweb.common.Full

import net.liftweb.util.Helpers.tryo

import net.liftweb.util.FieldError

import net.liftweb.record.MetaRecord
import net.liftweb.record.Record
import net.liftweb.record.field.IntField
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

object Stuff extends Stuff with MetaRecord[Stuff]
{
    def findByID(id: Int): Box[Stuff] = inTransaction {
        tryo { BeDoneSchema.stuffs.where(_.idField === id).single }
    }

    def findByUser(user: User): Box[List[Stuff]] = inTransaction {
        tryo {
            from(BeDoneSchema.stuffs)(table =>
                where(table.userID === user.idField) 
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
    val idField = new IntField(this, 1)
    val userID = new IntField(this)
    val createTime = new DateTimeField(this)
    val isTrash = new BooleanField(this, false)

    val title = new StringField(this, "") {
        override def displayName = "標題"
        override def validations = valMinLen(1, "此為必填欄位")_ :: super.validations
    }
    val description = new TextareaField(this, 1000) {
        override def displayName = "描述"
    }
    val deadline = new OptionalDateTimeField(this) {
        override def displayName = "期限"
        override def helpAsHtml = Full(scala.xml.Text("格式為 yyyy-MM-dd"))
    }

    def topics = StuffTopic.findByStuff(this)

    def projects = inTransaction(tryo {
        BeDoneSchema.stuffProjects.where(_.stuffID === this.idField).map(_.project).toList
    })

    def descriptionHTML = {
        import org.tautua.markdownpapers.Markdown
        val reader = new StringReader(description.is)
        val writer = new StringWriter
        val markdown = new Markdown
        markdown.transform(reader, writer)

        val rawHTML = "<div>" + writer.toString + "</div>"
        scala.xml.XML.loadString(rawHTML)
    }

    override def saveTheRecord() = inTransaction { tryo(BeDoneSchema.stuffs.insert(this)) }

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

        projectTitles.map(getProject).foreach(_.addStuff(this))
    }
}


