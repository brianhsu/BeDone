package org.bedone.snippet

import org.bedone.model._

import net.liftweb.common.Box
import net.liftweb.common.Empty
import net.liftweb.common.Full
import net.liftweb.common.Failure

import net.liftweb.util.Helpers._
import net.liftweb.http.SHtml
import net.liftweb.http.S

import scala.xml.NodeSeq

class Login
{
    private var username: Box[String] = Empty
    private var password: Box[String] = Empty

    private implicit def fromString(s: String) = s match {
        case null | ""  => Empty
        case text => Full(text)
    }

    def login() = {

        val user = for {
            username <- this.username
            password <- this.password
            user <- User.findByUsername(username) if user.password.match_?(password)
        } yield user

        user.isDefined match {
            case true => user.open_!.login(S.redirectTo("/dashboard"))
            case false => S.error("帳號密碼錯誤")
        }
    }

    def logoutButton = CurrentUser.isDefined match {
        case false => "*" #> ""
        case true  => "button" #> SHtml.button("登出", logout _)
    }

    def loginForm = CurrentUser.isDefined match {
        case true  => "*" #> ""
        case false =>
            "name=username" #> SHtml.text("", username = _) &
            "name=password" #> SHtml.password("", password = _) &
            "type=submit" #> SHtml.submit("Sign in", login _)
    }

    def logout() = {
        CurrentUser.foreach { _.logout(S.redirectTo("/")) }
    }

    def redirectToHome = {

        if (CurrentUser.isDefined) {
            S.redirectTo("/dashboard")
        }

        "*" #> ""
    }

}
