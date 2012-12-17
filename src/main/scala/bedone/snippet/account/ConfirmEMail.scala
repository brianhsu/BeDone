package org.bedone.snippet

import org.bedone.model._
import org.bedone.lib._

import net.liftweb.common._

import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.Templates
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmd

import net.liftweb.util.Helpers._
import net.liftweb.util.PassThru

import java.util.Calendar
import java.util.Date

class ConfirmEMail
{
    val user = {
        for (
            username <- S.param("username") ~> S.?("Username is required.");
            code <- S.param("code") ~> S.?("Activation code is required.");
            user <- User.findByUsername(username) if user.activationCode.is == Some(code) 
        ) yield user
    }

    def render = {

        user match {
            case Full(user) => {
                user.activate()
                println(user.saveTheRecord())
                S.notice(S.?("This account is activated. You could login to BeDone with your username and password now!"))
            }
            case Empty => S.error(S.?("Activation code is incorrect, please check your activation URL again."))
            case Failure(_, _, msg) => S.error(S.?("Cannot activate this account:") + msg)
        }

        PassThru
    }
}
