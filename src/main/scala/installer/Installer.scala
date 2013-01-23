package org.bedone.installer

import java.io.File
import java.io.PrintWriter
import java.io.FileWriter

import jline.ConsoleReader
import net.liftweb.util.StringHelpers

object DBCreator
{
    val consoleReader = new ConsoleReader

    consoleReader.setBellEnabled(false)

    def getYesOrNo(prompt: String): String =
    {
        val line = Option(consoleReader.readLine(prompt, '\0'))

        line.filter(x => x == "yes" || x == "no" ) match {
            case Some(text) => text
            case None => getYesOrNo(prompt)
        }
    }

    def createDBSchema()
    {
        import bootstrap.liftweb.Boot
        import org.bedone.model.BeDoneSchema
        import net.liftweb.squerylrecord.RecordTypeMode._
        import net.liftweb.squerylrecord.SquerylRecord._

        (new Boot).boot

        print("Create DB schema...")
        inTransaction { BeDoneSchema.drop }
        inTransaction { BeDoneSchema.create }
        println("Done")
    }

    def main(args: Array[String])
    {
        val shouldCreateDB = getYesOrNo("Do you want create empty BeDone DB schema? (yes/no)")

        shouldCreateDB match {
            case "yes" => createDBSchema()
            case _     => println("No action")
        }
    }
}

object Insteller
{
    val consoleReader = new ConsoleReader

    consoleReader.setBellEnabled(false)

    def getYesOrNo(prompt: String): String =
    {
        val line = Option(consoleReader.readLine(prompt, '\0'))

        line.filter(x => x == "yes" || x == "no" ) match {
            case Some(text) => text
            case None => getLine(prompt)
        }
    }

    def getLine(prompt: String): String =
    {
        val line = Option(consoleReader.readLine(prompt, '\0'))

        line.filter(_.length > 0) match {
            case Some(text) => text
            case None => getLine(prompt)
        }
    }

    def getLine(prompt: String, defaultValue: String): String =
    {
        val line = Option(consoleReader.readLine(prompt, '\0'))

        line.filter(_.length > 0) match {
            case Some(text) => text
            case None => defaultValue
        }
    }

    val configTemplate = """
        |# Database Setting
        |DatabaseURL = %s
        |DatabaseUsername = %s
        |DatabasePassword = %s
        |
        |# This is used to encrypt the GMail password in database
        |EncKey = %s
        |
        |# SMTP mail server setting
        |mail.transport.protocol= smtp
        |mail.smtp.host = %s
        |mail.smtp.port = %s
        |mail.smtp.starttls.enable = %s
        |mail.smtp.auth = %s
        |mail.user = %s
        |mail.password = %s
        |
        |# Google OAuth API setting used by GMail contacts import.
        |#
        |# You need add the callback URI to your Authorized Redirect URIs 
        |# in Google API console.
        |#
        |# The URI should look like the following:
        |#
        |#   http://your.bedone.host/contact/import
        |#
        |googleAPI.clientID = %s
        |googleAPI.clientSecret = %s
        |
    """.stripMargin

    def buildConfigFile(configFile: File)
    {
        println(
            """
                | #################################################################
                | # Setup MySQL Database
                | #
                | # Please follow the instruction to setup your MySQL connection.
                | #
            """.stripMargin
        )
        val dbHost = getLine("Please enter your MySQL host [localhost]:", "localhost")
        val dbPort = getLine("Please enter your MySQL port [3306]:", "3306")
        val dbName = getLine("Please enter your MySQL database name:")
        val jdbcURL = "jdbc:mysql://%s:%s/%s" format(dbHost, dbPort, dbName)

        val dbUsername = getLine("Please enter your MySQL username:")
        val dbPassword = getLine("Please enter your MySQL password:")

        val encKey = StringHelpers.randomString(28)
        
        println(
            """
                | #################################################################
                | # Setup SMTP mail server
                | #
                | # Please follow the instruction to setup your SMTP mail server.
                | #
            """.stripMargin
        )
        val smtpHost = getLine("Please enter SMTP hostname [smtp.gmail.com]:", "smtp.gmail.com")
        val smtpPort = getLine("Please enter SMTP port [587]:", "587")
        val smtpStartTLS = getLine("Enable STARTTLS [true]:", "true")
        val smtpAuth = getLine("Is authenticate required [true]:", "true")
        val smtpUsername = getLine("Please enter SMTP username:")
        val smtpPassword = getLine("Please enter SMTP password:")

        println(
            """
                | #################################################################
                | # Google OAuth API setting used by GMail contacts import.
                | #
                | # You need add the callback URI to your Authorized Redirect URIs 
                | # in Google API console.
                | #
                | # The URI should look like the following:
                | #
                | #   http://your.bedone.host/contact/import
                | #
            """.stripMargin
        )

        val gmailOAuthID = getLine("Please enter your GMail OAuth ClientID:")
        val gmailOAuthSecret = getLine("Please enter your GMail Client Secret:")

        val configFileContent = configTemplate.format(
            jdbcURL, dbUsername, dbPassword, encKey,
            smtpHost, smtpPort, smtpStartTLS, smtpAuth, smtpUsername, smtpPassword,
            gmailOAuthID, gmailOAuthSecret
        )

        val printWriter = new PrintWriter(configFile)
        printWriter.println(configFileContent)
        printWriter.close()
    }

    def main(args: Array[String])
    {
        val configFile = new File("src/main/resources/default.props")

        configFile.exists match {
            case true  => 
                println("===============================")
                println("[note] You already have src/main/resources/default.props")
                println("[note] please edit it directly if you want to change your configuration.")
                println("===============================")
            case false => 
                buildConfigFile(configFile)
        }
    }
}
