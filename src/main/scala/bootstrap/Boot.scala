// Filename: BeDone/src/main/scala/bootstrap/Boot.scala

package bootstrap.liftweb

import org.bedone.model.User
import org.bedone.view.AutoComplete

import net.liftweb.common.Full

import net.liftweb.http.LiftRules
import net.liftweb.http.Html5Properties
import net.liftweb.http.OldHtmlProperties
import net.liftweb.http.XHtmlInHtml5OutProperties
import net.liftweb.http.Req

import net.liftweb.record.field.PasswordField
import net.liftweb.sitemap.SiteMap
import net.liftweb.sitemap.Menu
import net.liftweb.sitemap.Loc._
import net.liftweb.http.S
import net.liftweb.util.LoanWrapper
import net.liftweb.squerylrecord.RecordTypeMode._

import java.io.Writer
import scala.xml.Node

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

        def siteMap = SiteMap(
            Menu.i("Index") / "index",
            (Menu.i("Dashboard") / "dashboard") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Inbox") / "inbox") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Action") / "nextAction") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Delegated") / "delegated") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Scheduled") / "scheduled") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Maybe") / "maybe") >> If(User.isLoggedIn _, "請先登入"),
            (Menu.i("Reference") / "reference") >> If(User.isLoggedIn _, "請先登入")
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

