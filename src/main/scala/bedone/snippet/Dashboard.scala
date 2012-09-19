package org.bedone.snippet

import net.liftweb.common.Box
import net.liftweb.common.Empty
import net.liftweb.common.Full

import net.liftweb.util.Helpers._
import net.liftweb.http.S
import net.liftweb.http.SessionVar

import org.scribe.builder._
import org.scribe.builder.api._
import org.scribe.model._

object ContactsOAuth extends SessionVar[Box[GMailContacts]](Empty)

class GMailContacts
{
    import scala.xml.XML

    val clientID = "32168263492-s20ia0f4pl30cu60dnbu099rdqj6uieu.apps.googleusercontent.com"
    val clientSecret = "Uo3VizoTG4yBGTVp-Q8XrhOT"
    val callbackURL = "http://localhost:8081/contactOAuth"

    println("S.host = " + S.hostName)

    private var accessToken: Box[Token] = Empty
    private val service = new ServiceBuilder()
                  .provider(classOf[GoogleApi])
                  .apiKey(clientID)
                  .apiSecret(clientSecret)
                  .callback(callbackURL)
                  .scope("https://www.google.com/m8/feeds/")
                  .build()

    private lazy val requestToken = service.getRequestToken

    def authURL = service.getAuthorizationUrl(requestToken)

    def setAccessToken(verifierCode: String) {
        
        if (!accessToken.isDefined) {
            val verifier = new Verifier(verifierCode)
            accessToken = tryo(service.getAccessToken(requestToken, verifier))
        }
    }

    def contacts = {
        val apiURL = "https://www.google.com/m8/feeds/contacts/default/full?max-results=10000"
        val request = new OAuthRequest(Verb.GET, apiURL)

        request.addHeader("GData-Version", "3.0")
        service.signRequest(accessToken.get, request)

        val response = request.send()
        val contactsXML = XML.loadString(response.getBody) \\ "entry"

        contactsXML.map { entry =>
            val id = (entry \ "id").text
            val name = (entry \ "title").text
            val phone = (entry \ "phoneNumber").headOption.map(_.text)
            val address = (entry \ "formattedAddress").headOption.map(_.text)
            val email = (entry \ "email").filter(x => (x \ "@primary").text == "true")
                                         .map(x => (x \ "@address").text)
                                         .headOption

            (id, name, phone, address, email)
        }
    }
}

class DashBoard
{
    def render = {
        val contactsAPI = new GMailContacts

        ContactsOAuth(Full(contactsAPI))
        S.redirectTo(contactsAPI.authURL)

        "aaa" #> "QQQ"
    }
}
