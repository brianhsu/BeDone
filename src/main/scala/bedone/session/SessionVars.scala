package org.bedone.session

import org.bedone.lib.GMailContacts

import net.liftweb.common.Box
import net.liftweb.common.Empty
import net.liftweb.http.SessionVar

object ContactsOAuth extends SessionVar[Box[GMailContacts]](Empty)

