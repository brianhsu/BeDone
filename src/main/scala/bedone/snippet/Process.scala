package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import org.bedone.model.StuffType.StuffType
import TagButton.Implicit._

import net.liftweb.common.Box
import net.liftweb.common.Empty
import net.liftweb.common.Full
import net.liftweb.common.Failure

import net.liftweb.util.Helpers._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.jquery.JqJsCmds._

import java.util.Calendar
import java.text.SimpleDateFormat

class Process extends JSImplicit
{
    private implicit def optFromStr(x: String) = Option(x).filterNot(_.trim.length == 0)

    private lazy val currentUser = CurrentUser.is.get
    private lazy val stuff = Stuff.findByUser(currentUser).openOr(Nil)
                                  .filterNot(_.isTrash.is).headOption

    private var currentProjects = stuff.map(_.projects).getOrElse(Nil)
    private var currentTopics = stuff.map(_.topics).getOrElse(Nil)

    // Stuff attirbute
    private var projectTitle: Option[String] = None
    private var topicTitle: Option[String] = None

    // Action attribute
    private var actionTitle: Option[String] = stuff.map(_.title.is)

    // Delegated attribute
    private var contactName: Option[String] = None

    // Scheduled attribute
    private var startDateTime: Option[Calendar] = None
    private var endDateTime: Option[Calendar] = None
    private var location: Option[String] = None

    private lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")

    def onTopicClick(buttonID: String, topic: Topic) = Noop
    def onProjectClick(buttonID: String, project: Project) = Noop

    def onProjectRemove(buttonID: String, project: Project): JsCmd = {

        currentProjects = currentProjects.filterNot(_ == project)
        FadeOutAndRemove.byClassName("project" + project.idField.is)
    }

    def onTopicRemove(buttonID: String, topic: Topic): JsCmd = {

        currentTopics = currentTopics.filterNot(_ == topic)
        FadeOutAndRemove.byClassName("topic" + topic.idField.is)
    }

    def saveDelegated(stuff: Stuff)(valueAttr: String): JsCmd = {

        def createContact = 
            Contact.createRecord.userID(currentUser.idField.is).name(contactName.get)

        contactName match {
            case None => Alert("負責人為必填欄位")
            case Some(name) =>
                stuff.setTopics(currentTopics)
                stuff.setProjects(currentProjects)
                stuff.stuffType(StuffType.Delegated)
                stuff.saveTheRecord()

                val contact = Contact.findByName(currentUser, name)
                                     .openOr(createContact)
                                     .saveTheRecord().get

                val action = Action.createRecord.idField(stuff.idField.is)
                                   .doneTime(Calendar.getInstance)
                                   .saveTheRecord().get

                val delegated = Delegated.createRecord.idField(stuff.idField.is)
                                         .contactID(contact.idField.is)
                                         .saveTheRecord()
                
                S.redirectTo(
                    "/process", () => S.notice("已將「%s」加入指派清單" format(stuff.title.is))
                )
               
        }
    }

    def saveReference(stuff: Stuff)(valueAttr: String) = {

        updateStuff(stuff, StuffType.Reference)

        S.redirectTo(
            "/process", () => S.notice("已將「%s」加入參考資料" format(stuff.title.is))
        )
    }

    def markAsTrash(stuff: Stuff)(valueAttr: String) = {

        stuff.isTrash(true).saveTheRecord()
        S.redirectTo("/process", () => S.notice("已刪除「%s」" format(stuff.title.is)))
    }

    def markAsDone(stuff: Stuff)(valueAttr: String) = {

        val updatedStuff = updateStuff(stuff, StuffType.Action)

        Action.createRecord.idField(updatedStuff.idField.is)
              .isDone(true).doneTime(Calendar.getInstance)
              .saveTheRecord()

        S.redirectTo("/process", () => S.notice("已將「%s」標記為完成" format(stuff.title.is)))
    }

    def addProject(title: String): JsCmd = {

        val containers = List("referenceProject", "maybeProject", "nextActionProject")
        val userID = CurrentUser.get.get.idField.is

        def createProject = Project.createRecord.userID(userID).title(title)
        def project = Project.findByTitle(userID, title).getOrElse(createProject)

        currentProjects.contains(project) match {
            case true  => ClearValue.byClassName("projectInput")
            case false =>
                currentProjects ::= project

                ClearValue.byClassName("projectInput") &
                containers.map { htmlID => 
                    AppendHtml(htmlID, project.editButton(onProjectClick, onProjectRemove))
                }
        }
    }

    def addProject(): JsCmd = {

        projectTitle match {
            case None        => Noop
            case Some(title) => addProject(title)
        }
    }
    
    def addTopic(title: String): JsCmd = {
        val containers = List("referenceTopic", "maybeTopic", "nextActionTopic")
        val userID = CurrentUser.get.get.idField.is

        def createTopic = Topic.createRecord.userID(userID).title(title)
        def topic = Topic.findByTitle(userID, title).getOrElse(createTopic)

        currentTopics.contains(topic) match {
            case true  => ClearValue.byClassName("topicInput")
            case false =>
                currentTopics ::= topic

                ClearValue.byClassName("topicInput") &
                containers.map { htmlID => 
                    AppendHtml(htmlID, topic.editButton(onTopicClick, onTopicRemove))
                }
        }
    }

    def addTopic(): JsCmd = {
        topicTitle match {
            case None        => Noop
            case Some(title) => addTopic(title)
        }
    }

    def createTopicTags(containerID: String) =
    {
        "name=topicInput"   #> SHtml.text("", topicTitle = _) &
        ".topicInputHidden" #> SHtml.hidden(addTopic) &
        ".topicTags [id]"   #> containerID &
        ".topicTags" #> (
            "span" #> currentTopics.map(_.editButton(onTopicClick, onTopicRemove))
        )
    }

    def createProjectTags(containerID: String) =
    {
        "name=projectInput"   #> SHtml.text("", projectTitle = _) &
        ".projectInputHidden" #> SHtml.hidden(addProject) &
        ".projectTags [id]"   #> containerID &
        ".projectTags" #> (
            "span" #> currentProjects.map(_.editButton(onProjectClick, onProjectRemove))
        )
    }

    def doNothing(s: String) {}

    def setContact(name: String): JsCmd = {
        this.contactName = name
        this.contactName match {
            case None    => "$('#saveDelegated').attr('disabled', true)"
            case Some(t) => "$('#saveDelegated').attr('disabled', false)"
        }
    }

    def setTitle(title: String): JsCmd = {

        val stuffTitle = stuff.map(_.title.is).getOrElse("")

        this.actionTitle = title
        this.actionTitle match {
            case None       => "$('#nextActionTitle').val('%s');".format(stuffTitle)
            case Some(text) => Noop
        }
    }

    def updateStuff(stuff: Stuff, stuffType: StuffType) =
    {
        stuff.title(actionTitle.getOrElse(stuff.title.is))
        stuff.stuffType(stuffType)
        stuff.setProjects(currentProjects)
        stuff.setTopics(currentTopics)
        stuff
    }

    def saveScheduled(stuff: Stuff)(valueAttr: String): JsCmd = {

        val updatedStuff = updateStuff(stuff, StuffType.Scheduled)
        val action = Action.createRecord.idField(updatedStuff.idField.is)
        val scheduled = Scheduled.createRecord.idField(updatedStuff.idField.is)

        scheduled.startTime.setBox(startDateTime)
        scheduled.endTime.setBox(endDateTime)
        scheduled.location.setBox(location)
        scheduled.validate match {
            case Nil => 
                stuff.saveTheRecord()
                action.saveTheRecord()
                scheduled.saveTheRecord()
                S.redirectTo(
                    "/process", 
                    () => S.notice("已將「%s」放入行事曆" format(stuff.title.is))
                )

                Noop
            case xs  => Alert(xs.map(_.msg).mkString("、"))
        }
    }

    def setEndDateTime(dateTimeString: String): JsCmd = {

        endDateTime = optFromStr(dateTimeString) match {
            case None => None
            case Some(dateTimeString) =>
                try {
                    dateTimeFormatter.setLenient(false)
                    val dateTime = dateTimeFormatter.parse(dateTimeString)
                    val calendar = Calendar.getInstance
                    calendar.setTime(dateTime)
                    Some(calendar)
                } catch {
                    case e => println("error:" + e)
                        None
                }
        }

        Noop

    }

    def setStartDateTime(dateTimeString: String): JsCmd = {
        

        val dateTime = tryo {
            require(optFromStr(dateTimeString).isDefined, "此為必填欄位")

            dateTimeFormatter.setLenient(false)

            val dateTime = dateTimeFormatter.parse(dateTimeString)
            val calendar = Calendar.getInstance
            calendar.setTime(dateTime)
            calendar
        }

        this.startDateTime = dateTime.toOption

        dateTime match {
            case Full(date) => "$('#saveScheduled').attr('disabled', false)"
            case Empty => "$('#saveScheduled').attr('disabled', true)"
            case Failure(msg, _, _) => "$('#saveScheduled').attr('disabled', true)"
        }
    }

    def hasStuffBinding(stuff: Stuff) = 
    {
        "#noStuffAlert" #> "" &
        "#isTrash [onclick]" #> SHtml.onEvent(markAsTrash(stuff)) &
        "#markAsDone [onclick]" #> SHtml.onEvent(markAsDone(stuff)) &
        "#location" #> SHtml.textAjaxTest("", doNothing _, location = _) &
        "#startDateTime" #> SHtml.textAjaxTest("", doNothing _, setStartDateTime _) &
        "#endDateTime" #> SHtml.textAjaxTest("", doNothing _, setEndDateTime _) &
        "#saveScheduled [onclick]" #> SHtml.onEvent(saveScheduled(stuff)) &
        "#isDelegated" #> (
            ".contactInput" #> SHtml.textAjaxTest("", doNothing _, setContact _) &
            "#saveDelegated [onclick]" #> SHtml.onEvent(saveDelegated(stuff))
        ) &
        "#itIsReference" #> (
            createTopicTags("referenceTopic") &
            createProjectTags("referenceProject") &
            "#saveReference [onclick]" #> SHtml.onEvent(saveReference(stuff))
        ) &
        "#itIsMaybe" #> (
            createTopicTags("maybeTopic") &
            createProjectTags("maybeProject")
        ) &
        "#whatIsNextAction" #> (
            createTopicTags("nextActionTopic") &
            createProjectTags("nextActionProject") &
            "name=nextActionTitle" #> 
                SHtml.textAjaxTest(stuff.title.is, doNothing _, setTitle _)
        )
    }

    def noStuffBinding = 
    {
        ".dialog" #> ""
    }

    def render = {
        stuff match {
            case Some(stuff) => hasStuffBinding(stuff)
            case None        => noStuffBinding
        }
    }
}
