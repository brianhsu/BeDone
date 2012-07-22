// Filename: BeDone/src/main/scala/bootstrap/Boot.scala

package bootstrap.liftweb

import net.liftweb.common.Full

import net.liftweb.http.LiftRules
import net.liftweb.http.Html5Properties
import net.liftweb.http.Req
import net.liftweb.record.field.PasswordField

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

        // 使用 HTML 5 做為模版
        LiftRules.htmlProperties.default.set { r: Req => 
            new Html5Properties(r.userAgent)
        }

        initAjaxLoader()
        PasswordField.minPasswordLength = 7

        DBSettings.initDB()
    }
}

