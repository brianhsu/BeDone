package org.bedone.installer

import java.io.File
import java.io.PrintWriter
import java.io.FileWriter

import net.liftweb.util.StringHelpers


object Insteller
{
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

    val dbSystem = ConsoleReader.getChoice("Please enter your DB system", List("mysql", "postgres"))
    val (defaultPort, jdbcURLPattern) = dbSystem match {
      case "mysql" => ("3306", "jdbc:mysql://%s:%s/%s")
      case "postgres" => ("5432", "jdbc:postgresql://%s:%s/%s")
    }

    val dbHost = ConsoleReader.getLine("Please enter your DB host [localhost]:", "localhost")
    val dbPort = ConsoleReader.getLine("Please enter your DB port [5432]:", defaultPort)
    val dbName = ConsoleReader.getLine("Please enter your DB database name:")
    val jdbcURL = jdbcURLPattern.format(dbHost, dbPort, dbName)

    val dbUsername = ConsoleReader.getLine("Please enter your DB username:")
    val dbPassword = ConsoleReader.getLine("Please enter your DB password:")

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

    val smtpHost = ConsoleReader.getLine("Please enter SMTP hostname [smtp.gmail.com]:", "smtp.gmail.com")
    val smtpPort = ConsoleReader.getLine("Please enter SMTP port [587]:", "587")
    val smtpStartTLS = ConsoleReader.getLine("Enable STARTTLS [true]:", "true")
    val smtpAuth = ConsoleReader.getLine("Is authenticate required [true]:", "true")
    val smtpUsername = ConsoleReader.getLine("Please enter SMTP username:")
    val smtpPassword = ConsoleReader.getLine("Please enter SMTP password:")

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

    val gmailOAuthID = ConsoleReader.getLine("Please enter your GMail OAuth ClientID:")
    val gmailOAuthSecret = ConsoleReader.getLine("Please enter your GMail Client Secret:")

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
    val runMode = ConsoleReader.getChoice("Run mode", List("development", "production", "test"))

    val configFile = runMode match {
      case "development" => new File("src/main/resources/default.props")
      case "production" => new File("src/main/resources/production.default.props")
      case "test" => new File("src/main/resources/test.default.props")
    }

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
