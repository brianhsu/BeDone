package org.bedone.lib

import org.bedone.model.Contact

import net.liftweb.common.Box
import net.liftweb.common.Empty

import net.liftweb.http.SessionVar
import net.liftweb.util.Helpers.tryo
import net.liftweb.http.S

import org.scribe.builder._
import org.scribe.builder.api._
import org.scribe.model._

import scala.xml.XML

class GMailContacts
{
    val scope = "https://www.google.com/m8/feeds/"
    val clientID = "32168263492-s20ia0f4pl30cu60dnbu099rdqj6uieu.apps.googleusercontent.com"
    val clientSecret = "Uo3VizoTG4yBGTVp-Q8XrhOT"
    val callbackURL = "%s/contact/import" format(S.hostAndPath)
    val apiURL = scope + "contacts/default/full?max-results=10000"

    private var accessToken: Box[Token] = Empty
    private val service = new ServiceBuilder()
                  .provider(classOf[Google2Api])
                  .apiKey(clientID)
                  .apiSecret(clientSecret)
                  .callback(callbackURL)
                  .scope(scope)
                  .build()

    def authURL = service.getAuthorizationUrl(null)

    def setAccessToken(verifierCode: String) {
        
        if (!accessToken.isDefined) {
            val verifier = new Verifier(verifierCode)
            accessToken = tryo(service.getAccessToken(null, verifier))
        }
    }

    def contacts = tryo {
        val request = new OAuthRequest(Verb.GET, apiURL)

        request.addHeader("GData-Version", "3.0")
        service.signRequest(accessToken.get, request)

        val response = request.send().getBody
        val contactsXML = XML.loadString(response) \\ "entry"
        val contacts = contactsXML.map { entry =>
            val googleID = (entry \ "id").text
            val name = (entry \ "title").text
            val phone = (entry \ "phoneNumber").headOption.map(_.text)
            val address = (entry \\ "formattedAddress").headOption.map(_.text)
            val email = (entry \ "email").filter(x => (x \ "@primary").text == "true")
                                         .map(x => (x \ "@address").text)
                                         .headOption

            val contact = Contact.createRecord

            contact.googleID(googleID)
                   .name(name)
                   .phone(phone).email(email)
                   .address(address)

            contact
        }

        contacts.filterNot(_.name.is == "")
    }
}

