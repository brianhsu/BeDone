package bootstrap.liftweb

import net.liftweb.util.Props
import net.liftweb.squerylrecord.SquerylRecord

import org.squeryl.Session
import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.adapters.MySQLAdapter

import com.jolbox.bonecp.BoneCP
import com.jolbox.bonecp.BoneCPConfig

import java.sql.DriverManager

object DBSettings
{
  private val dbURL = Props.get("DatabaseURL").openOr("jdbc:mysql://localhost:3306/BeDone")
  private val dbUsername = Props.get("DatabaseUsername").openOr("username")
  private val dbPassword = Props.get("DatabasePassword").openOr("password")
  private val dbSystem = dbURL match {
    case url if url.contains("mysql")      => 'MySQL
    case url if url.contains("postgresql") => 'PostgresSQL
  }

  private lazy val connectionPool = {
    val config = new BoneCPConfig()
    config.setJdbcUrl(dbURL)
    config.setUsername(dbUsername)
    config.setPassword(dbPassword)
    config.setMinConnectionsPerPartition(5)
    config.setMaxConnectionsPerPartition(10)
    config.setPartitionCount(1)

    new BoneCP(config)
  }

  def initDB()
  {
    val adapter = dbSystem match {
      case 'MySQL =>
        Class.forName("org.postgresql.Driver")
        new MySQLAdapter
      case 'PostgresSQL =>
        Class.forName("com.mysql.jdbc.Driver")
        new PostgreSqlAdapter
    }

    SquerylRecord.initWithSquerylSession {
      Session.create(connectionPool.getConnection(), adapter)
    }
  }
}
