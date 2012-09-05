package org.bedone.lib

import net.liftweb.util.Helpers.md5
import net.liftweb.util.Helpers.hexEncode

object Gravatar
{
    def avatarURL(email: String): String = {
        val avatarHash = hexEncode(md5(email.trim.toLowerCase.getBytes))
        "http://www.gravatar.com/avatar/%s?d=mm" format(avatarHash)
    }
}
