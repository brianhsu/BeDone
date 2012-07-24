package org.bedone.model

import org.squeryl.Schema
import net.liftweb.squerylrecord.RecordTypeMode._

object BeDoneSchema extends Schema {

    val users = table[User]("users")
    val stuffs = table[Stuff]("stuffs")

    // Unique and Index
    on(users) { user => declare(user.username defineAs unique, user.email defineAs unique) }

    // Foreign Keys
    oneToManyRelation(users, stuffs).via { (user, stuff) => user.id === stuff.userID }


}    
