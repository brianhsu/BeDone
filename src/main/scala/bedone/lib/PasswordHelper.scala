package org.bedone.lib

import net.liftweb.util.SecurityHelpers._
import net.liftweb.util.Props

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec

object PasswordHelper
{
    private val encKey = Props.get("EncKey").openOr("&dfk-as#@!23;9fds)*x")

    def createCipher(mode: Int) = 
    {
        val salt = hash(encKey).take(8).getBytes
        val keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
        val password = keyFactory.generateSecret(new PBEKeySpec(encKey.toCharArray))
        val cipher = Cipher.getInstance("PBEWithMD5AndDES")

        cipher.init(mode, password, new PBEParameterSpec(salt, 103))
        cipher
    }

    def encrypt(plainText: String) = 
    {
        val cipher = createCipher(Cipher.ENCRYPT_MODE)
        base64Encode(cipher.doFinal(plainText.getBytes))
    }

    def decrypt(secretText: String): Option[String] = 
    {
        try {
            val cipher = createCipher(Cipher.DECRYPT_MODE)
            Some(cipher.doFinal(base64Decode(secretText)).map(_.toChar).mkString)
        } catch {
            case e: Exception => None
        }
    }
}
