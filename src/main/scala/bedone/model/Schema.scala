package org.bedone.model

import org.squeryl.Schema
import net.liftweb.squerylrecord.RecordTypeMode._

object BeDoneSchema extends Schema {

    val users = table[User]("users")
    val stuffs = table[Stuff]("stuffs")
    val stuffTopics = table[StuffTopic]("stuff_topics")

    // Unique and Index
    on(users) { user => declare(user.username defineAs unique, user.email defineAs unique) }
    on(stuffTopics) { stuffTopic => declare(
        columns(stuffTopic.stuffID, stuffTopic.topic) are unique
    )}

    // Foreign Keys
    oneToManyRelation(users, stuffs).via { (user, stuff) => user.id === stuff.userID }
    oneToManyRelation(stuffs, stuffTopics).via { 
        (stuff, stuffTopic) => stuff.id === stuffTopic.stuffID 
    }
}    
