// Filename: BeDone/src/main/scala/bootstrap/Boot.scala

package bootstrap.liftweb

import org.bedone.model._

import org.bedone.view.AutoComplete

import net.liftweb.common.Full

import net.liftweb.http.LiftRules
import net.liftweb.http.Html5Properties
import net.liftweb.http.OldHtmlProperties
import net.liftweb.http.XHtmlInHtml5OutProperties
import net.liftweb.http.Req
import net.liftweb.http.Templates
import scala.xml.NodeSeq

import net.liftweb.record.field.PasswordField
import net.liftweb.sitemap.SiteMap
import net.liftweb.sitemap.Menu
import net.liftweb.sitemap.Loc._
import net.liftweb.sitemap._
import net.liftweb.http.S

import net.liftweb.util.Props
import net.liftweb.util.LoanWrapper
import net.liftweb.util.Mailer
import net.liftweb.util.Mailer._

import net.liftweb.squerylrecord.RecordTypeMode._

import net.liftmodules.combobox._
import javax.mail.{Authenticator,PasswordAuthentication}

class Boot 
{
    def initAjaxLoader()
    {
        LiftRules.ajaxStart = Full(() => {
            LiftRules.jsArtifacts.show("ajax-loader").cmd &
            LiftRules.jsArtifacts.show("ajax-loader-signup").cmd
        })
        LiftRules.ajaxEnd = Full(() => {
            LiftRules.jsArtifacts.hide("ajax-loader").cmd &
            LiftRules.jsArtifacts.hide("ajax-loader-signup").cmd
        })
    }

    def siteMap = 
    {
        // Create a menu for /param/somedata
        val contactDetail = Menu.param[Contact](
            "Contact Detail", "Contact Detail", 
            parser = Contact.paramParser _ , 
            encoder = _.idField.is.toString
        ) / "contact" / *

        val projectDetail = Menu.param[Project](
            "Project Detail", "Project Detail",
            parser = Project.paramParser _,
            encoder = _.idField.is.toString
        ) / "project" / *

        val topicDetail = Menu.param[Topic](
            "Topic Detail", "Topic Detail",
            parser = Topic.paramParser _,
            encoder = _.idField.is.toString
        ) / "topic" / *

        SiteMap(
            Menu.i("Index") / "index",

            (Menu.i("ConfirmEMail") / "account" / "confirmEMail") >> Hidden,
            (Menu.i("ForgetPassword") / "account" / "forgetPassword") >> Hidden,
            (Menu.i("ResetPassword") / "account" / "resetPassword") >> Hidden,

            (Menu.i("Preference") / "preference") >> If(User.isLoggedIn _, "請先登入"),

            (Menu.i("Dashboard") / "dashboard") >> If(User.isLoggedIn _, "請先登入"),

            (Menu.i("Inbox") / "todo" / "inbox") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Action") / "todo" / "nextAction") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Delegated") / "todo" / "delegated") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Scheduled") / "todo" / "scheduled") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Process") / "todo" / "process") >> If(User.isLoggedIn _, "請先登入"),

            (Menu.i("Project") / "review" / "project") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Topic") / "review" / "topic") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Maybe") / "review" / "maybe") >> If(User.isLoggedIn _, "請先登入"),

            (Menu.i("Trash") / "other" / "trash") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Reference") / "other" / "reference") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Contacts") / "contact") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Import Contacts") / "contact" / "import") >> If(User.isLoggedIn _, "請先登入"),
            (contactDetail >> Template(() => Templates("contact" :: "detail" :: Nil) openOr NodeSeq.Empty)),
            (projectDetail >> Template(() => Templates("project" :: "detail" :: Nil) openOr NodeSeq.Empty)),
            (topicDetail >> Template(() => Templates("topic" :: "detail" :: Nil) openOr NodeSeq.Empty))
        )
    }

    def initMailer()
    {
        val isAuth = Props.getBool("mail.smtp.auth", false)

        for (username <- Props.get("mail.user") if isAuth;
             password <- Props.get("mail.password"))
        {
            println("init mail password")

            Mailer.authenticator = Full(new Authenticator() {
                override def getPasswordAuthentication = {
                    new PasswordAuthentication(username, password)
                }
            })
        }
    }

    def boot 
    {
        // 我們的程式碼放在 org.bedone 這個 package 中
        LiftRules.addToPackages("org.bedone")
        LiftRules.setSiteMap(siteMap)
        LiftRules.dispatch.append(AutoComplete.autoComplete)
        LiftRules.htmlProperties.default.set { r: Req => 
            new XHtmlInHtml5OutProperties(r.userAgent)
        }

        PasswordField.minPasswordLength = 7
           
        S.addAround(new LoanWrapper{
            override def apply[T](f: => T): T = {
                inTransaction{
                    f
                }
            }
        })

        initMailer()
        initAjaxLoader()
        DBSettings.initDB()
        ComboBox.init
    }

}

