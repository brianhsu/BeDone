package bootstrap.liftweb

import net.liftweb.util.Props
import net.liftweb.squerylrecord.SquerylRecord

import org.squeryl.Session
import org.squeryl.adapters.MySQLAdapter

import java.sql.DriverManager

object DBSettings
{
    Class.forName("com.mysql.jdbc.Driver")

    private val dbURL = Props.get("DatabaseURL").open_!
    private val dbUsername = Props.get("DatabaseUsername").open_!
    private val dbPassword = Props.get("DatabasePassword").open_!

    def initDB()
    {
        val adapter = new MySQLAdapter

        SquerylRecord.initWithSquerylSession(Session.create(
            DriverManager.getConnection(dbURL, dbUsername, dbPassword), adapter
        ))
    }
}
