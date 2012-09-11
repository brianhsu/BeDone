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
import net.liftweb.util.LoanWrapper
import net.liftweb.squerylrecord.RecordTypeMode._

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

    def boot 
    {
        // 我們的程式碼放在 org.bedone 這個 package 中
        LiftRules.addToPackages("org.bedone")

        // Create a menu for /param/somedata
        val contactDetail = Menu.param[Contact](
            "Contact Detail", "Contact Detail", 
            parser = Contact.paramParser _ , 
            encoder = _.idField.is.toString
        ) / "contact" / *

        val projectInbox = Menu.param[Project](
            "Project Detail", "Project Detail",
            parser = Project.paramParser _,
            encoder = _.idField.is.toString
        ) / "project" / * / "inbox"

        def siteMap = SiteMap(
            Menu.i("Index") / "index",
            (Menu.i("Dashboard") / "dashboard") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Inbox") / "inbox") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Action") / "nextAction") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Delegated") / "delegated") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Scheduled") / "scheduled") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Maybe") / "maybe") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Reference") / "reference") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Preference") / "preference") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Process") / "process") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Contacts") / "contact") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Project") / "project") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Topic") / "topic") >> If(User.isLoggedIn _, "請先登入"),
            (contactDetail >> Template(() => Templates("contact" :: "detail" :: Nil) openOr NodeSeq.Empty)),
            (projectInbox >> Template(() => Templates("project" :: "detail" :: Nil) openOr NodeSeq.Empty))
        )

        LiftRules.setSiteMap(siteMap)

        initAjaxLoader()
        PasswordField.minPasswordLength = 7

        LiftRules.dispatch.append(AutoComplete.autoComplete)

        LiftRules.htmlProperties.default.set { r: Req => 
            new XHtmlInHtml5OutProperties(r.userAgent)
        }
           
        S.addAround(new LoanWrapper{
            override def apply[T](f: => T): T = {
                inTransaction{
                    f
                }
            }
        })

        DBSettings.initDB()
    }

}

