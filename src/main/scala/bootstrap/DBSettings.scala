package bootstrap.liftweb

import net.liftweb.util.Props
import net.liftweb.squerylrecord.SquerylRecord

import org.squeryl.Session
import org.squeryl.adapters.PostgreSqlAdapter

import java.sql.DriverManager

import com.jolbox.bonecp.BoneCP
import com.jolbox.bonecp.BoneCPConfig

object DBSettings
{
    Class.forName("org.postgresql.Driver")

    private val dbURL = Props.get("DatabaseURL").openOr("jdbc:postgresql://localhost:3306/BeDone")
    private val dbUsername = Props.get("DatabaseUsername").openOr("username")
    private val dbPassword = Props.get("DatabasePassword").openOr("password")

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
        val adapter = new PostgreSqlAdapter

        SquerylRecord.initWithSquerylSession {
            Session.create(connectionPool.getConnection(), adapter)
        }
    }
}
