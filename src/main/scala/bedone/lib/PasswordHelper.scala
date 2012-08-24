package org.bedone.lib

import net.liftweb.util.SecurityHelpers._

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec

object PasswordHelper
{
    def createCipher(key: String, mode: Int) = 
    {
        val salt = hash(key.reverse).take(8).getBytes
        val keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
        val password = keyFactory.generateSecret(new PBEKeySpec(key.toCharArray))
        val cipher = Cipher.getInstance("PBEWithMD5AndDES")

        cipher.init(mode, password, new PBEParameterSpec(salt, 103))
        cipher
    }

    def encrypt(key: String, plainText: String) = 
    {
        val cipher = createCipher(key, Cipher.ENCRYPT_MODE)
        base64Encode(cipher.doFinal(plainText.getBytes))
    }

    def decrypt(key: String, secretText: String): Option[String] = 
    {
        try {
            val cipher = createCipher(key, Cipher.DECRYPT_MODE)
            Some(cipher.doFinal(base64Decode(secretText)).map(_.toChar).mkString)
        } catch {
            case e => None
        }
    }
}
