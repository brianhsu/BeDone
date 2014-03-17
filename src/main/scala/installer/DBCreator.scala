package org.bedone.installer

import bootstrap.liftweb.Boot
import org.bedone.model.BeDoneSchema
import net.liftweb.squerylrecord.RecordTypeMode._
import net.liftweb.squerylrecord.SquerylRecord._

object DBCreator
{
  def createDBSchema()
  {
    (new Boot).boot

    print("Create DB schema...")
    inTransaction { BeDoneSchema.create }
    println("Done")
  }

  def main(args: Array[String])
  {
    val shouldCreateDB = ConsoleReader.getYesOrNo("Do you want create empty BeDone DB schema? (yes/no)")

    shouldCreateDB match {
      case "yes" => createDBSchema()
      case _     => println("No action")
    }
  }
}
