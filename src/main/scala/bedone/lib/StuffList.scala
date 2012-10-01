package org.bedone.lib

import org.bedone.model._
import org.bedone.lib._
import org.bedone.lib.TagButton.Implicit._
import org.bedone.snippet._

import net.liftweb.actor.LiftActor

import net.liftweb.common.Box
import net.liftweb.common.Full

import net.liftweb.util.Helpers._
import net.liftweb.util.Schedule
import net.liftweb.util.CssSel

import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.http.CometActor

import net.liftweb.squerylrecord.RecordTypeMode._

import java.text.SimpleDateFormat
import scala.xml.NodeSeq

trait StuffList extends JSImplicit
{
    val projectID: Box[Int]
    val topicID: Box[Int]

    protected var rapidTitle: String = _

    protected lazy val currentUser = CurrentUser.get.get
    protected lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm")
    protected lazy val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

    protected def allStuff = Stuff.findByUser(currentUser).openOr(Nil)
    protected def projectStuff = projectID.map(Stuff.findByProject(currentUser, _).openOr(Nil))
    protected def topicStuff = topicID.map(Stuff.findByTopic(currentUser, _).openOr(Nil))

    protected def stuffs = (projectStuff orElse topicStuff).getOrElse(allStuff)

    private var currentTopic: Option[Topic] = None
    private var currentProject: Option[Project] = None
    private var currentPage: Int = 1

    def formatDeadline(stuff: Stuff) = 
    {
        stuff.deadline.is match {
            case None => "*" #> ""
            case Some(calendar) => ".label *" #> dateFormatter.format(calendar.getTime)
        }
    }

    def actionBar(stuff: Stuff): CssSel = {

        def starClass = stuff.isStared.is match {
            case true  => "myicon-starOn"
            case false => "myicon-starOff"
        }

        def toogleStar(): JsCmd = {
            stuff.isStared(!stuff.isStared.is)
            stuff.saveTheRecord()
            
            """$('#inboxRow%s .star i').attr('class', '%s')""".format(stuff.idField, starClass)
        }

        def markAsTrash(): JsCmd = {
            stuff.isTrash(true)
            stuff.saveTheRecord()

            val newPaging = getPagedStuff
            val targetPage = (getPagedStuff.totalPage < currentPage) match {
                case true  => getPagedStuff.totalPage
                case false => currentPage
            }

            new FadeOut("inboxRow" + stuff.idField, 0, 500) &
            onSwitchPage(newPaging, targetPage)
        }

        val descIconVisibility = stuff.description.is.isEmpty match {
            case true  => "visibility:hidden"
            case false => "visibility:visible"
        }

        ".edit [onclick]" #> SHtml.onEvent(s => showEditForm(stuff)) &
        ".remove [onclick]" #> SHtml.onEvent(s => markAsTrash) &
        ".star [onclick]" #> SHtml.onEvent(s => toogleStar) &
        ".star" #> ("i [class]" #> starClass) &
        ".showDesc [data-target]" #> ("#inboxDesc" + stuff.idField) &
        ".showDesc [style+]" #> descIconVisibility
    }

    def onSwitchPage(paging: Paging[Stuff], page: Int) = {

        this.currentPage = page

        JqSetHtml("inboxList", createStuffTable(paging(page))) &
        JqSetHtml("inboxPageSelector", paging.pageSelector(page))
    }

    def getPagedStuff = 
    {
        def shouldDisplay(stuff: Stuff) = {
            currentTopic.map(t => stuff.hasTopic(t.idField.is)).getOrElse(true) &&
            currentProject.map(p => stuff.hasProject(p.idField.is)).getOrElse(true)
        }

        val currentStuffs = stuffs.filter(shouldDisplay)

        new Paging(Full(currentStuffs), 10, 5, onSwitchPage _)
    }

    def topicFilter(buttonID: String, topic: Topic): JsCmd = 
    {
        this.currentTopic = Some(topic)
        this.currentProject = None

        val paged = getPagedStuff
        val newTable = createStuffTable(paged(1))

        JqSetHtml("inboxList", newTable) &
        JqSetHtml("inboxCurrent", topic.title.is) &
        JqSetHtml("inboxPageSelector", paged.pageSelector(1)) &
        JsRaw("""$('#inboxShowAll').prop("disabled", false)""") &
        JsRaw("""$('#inboxCurrent').attr("class", "btn btn-info")""")
    }

    def projectFilter(buttonID: String, project: Project): JsCmd = 
    {
        this.currentTopic = None
        this.currentProject = Some(project)

        val paged = getPagedStuff
        val newTable = createStuffTable(paged(1))

        JqSetHtml("inboxList", newTable) &
        JqSetHtml("inboxCurrent", project.title.is) &
        JqSetHtml("inboxPageSelector", paged.pageSelector(1)) &
        JsRaw("""$('#inboxShowAll').prop("disabled", false)""") &
        JsRaw("""$('#inboxCurrent').attr("class", "btn btn-success")""")
    }

    def showAllStuff() = 
    {
        this.currentTopic = None
        this.currentProject = None

        val paged = getPagedStuff
        val newTable = createStuffTable(paged(1))

        JqSetHtml("inboxList", newTable) &
        JqSetHtml("inboxCurrent", "全部") &
        JqSetHtml("inboxPageSelector", paged.pageSelector(1)) &
        JsRaw("""$('#inboxShowAll').prop("disabled", true)""") &
        JsRaw("""$('#inboxCurrent').attr("class", "btn btn-inverse")""")
    }

    def createStuffTable(stuffs: List[Stuff]) = stuffs.map(createStuffRow).flatten

    def createStuffRow(stuff: Stuff) = 
    {
        def template = Templates("templates-hidden" :: "stuff" :: "item" :: Nil)

        val cssBinding = 
            actionBar(stuff) &
            ".stuffs [id]"   #> ("inboxRow" + stuff.idField) &
            ".collapse [id]" #> ("inboxDesc" + stuff.idField) &
            ".title *"       #> stuff.titleWithLink &
            ".desc *"        #> stuff.descriptionHTML &
            ".topic *"       #> stuff.topics.map(_.viewButton(topicFilter)).flatten &
            ".project *"     #> stuff.projects.map(_.viewButton(projectFilter)).flatten &
            ".deadline"      #> formatDeadline(stuff)

        template.map(cssBinding).openOr(<span>Template does not exists</span>)
    }

    def showInsertForm(): JsCmd = 
    {
        def userID = CurrentUser.is.map(_.idField.is).get
        def createNewStuff: Stuff = Stuff.createRecord.userID(userID)

        val editStuff = new EditStuffForm(createNewStuff, {stuff =>
            val newPaging = getPagedStuff
            onSwitchPage(newPaging, newPaging.totalPage)
        })

        """$('#stuffEdit').remove()""" &
        SetHtml("inboxEditHolder", editStuff.toForm)
    }

    def showEditForm(stuff: Stuff): JsCmd = 
    {
        val editStuff = new EditStuffForm(stuff, editPostAction _)

        """$('#stuffEdit').remove()""" &
        SetHtml("inboxEditHolder", editStuff.toForm)
    }

    def editPostAction(stuff: Stuff): JsCmd = 
    {
        val newRow = createStuffRow(stuff).flatMap(_.child)
        JqSetHtml("inboxRow" + stuff.idField.is, newRow)
    }

    def addRapidStuff(): JsCmd =
    {
        if (rapidTitle.length > 0) {
            val stuff = Stuff.createRecord.userID(currentUser.idField.is).title(rapidTitle)

            stuff.saveTheRecord()

            val newPaging = getPagedStuff

            """$('#inboxRapidStuff').val("")""" &
            onSwitchPage(newPaging, newPaging.totalPage)

        } else {
            Noop
        }
    }

    def cssBinding = 
    {
        val paging = getPagedStuff

        "#inboxShowAll" #> SHtml.ajaxButton("顯示全部", showAllStuff _) &
        "#inboxAdd [onclick]" #> SHtml.onEvent(s => showInsertForm) &
        "#inboxRapidStuff" #> SHtml.text("", rapidTitle = _) &
        "#inboxRapidTitle" #> SHtml.hidden(addRapidStuff _) &
        "#inboxPageSelector *" #> paging.pageSelector(1) &
        ".inboxRow" #> createStuffTable(paging(1))
    }
}

