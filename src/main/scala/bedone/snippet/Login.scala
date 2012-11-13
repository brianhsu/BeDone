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

        def isRegister(user: User) = user.activationStatus.value == ActivationStatus.Register
        def isReset(user: User) = user.activationStatus.value == ActivationStatus.Reset

        val authUser = for {
            username <- this.username
            password <- this.password
            user <- User.findByUsername(username) if user.password.match_?(password)
        } yield user

        authUser match {
            case Full(user) if isRegister(user) => S.error("請先使用 EMail 認證你的帳號")
            case Full(user) if isReset(user) => S.error("已將密碼重設郵件寄至您的 EMail，請檢查並重設密碼")
            case Full(user) => user.login(S.redirectTo("/dashboard"))
            case _          => S.error("帳號密碼錯誤")
        }
    }

    def logoutLink = CurrentUser.isDefined match {
        case false => "*" #> ""
        case true  => "#logout [onclick]" #> SHtml.onEvent(logout _)
    }

    def loginForm = CurrentUser.isDefined match {
        case true  => "*" #> ""
        case false =>
            "name=username" #> SHtml.text("", username = _) &
            "name=password" #> SHtml.password("", password = _) &
            "type=submit" #> SHtml.submit("Sign in", login _)
    }

    def logout(eventData: String) = {
        println("Hello World")
        CurrentUser.foreach { _.logout(S.redirectTo("/")) }
    }

    def redirectToHome = {

        if (CurrentUser.isDefined) {
            S.redirectTo("/dashboard")
        }

        "*" #> ""
    }

}
