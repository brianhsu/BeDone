package org.bedone.model

import org.squeryl.Schema
import net.liftweb.squerylrecord.RecordTypeMode._

object BeDoneSchema extends Schema {

    val users = table[User]("users")
    val stuffs = table[Stuff]("stuffs")
    val projects = table[Project]("projects")
    val stuffTopics = table[StuffTopic]("stuff_topics")
    val stuffProjects = table[StuffProject]("stuff_projects")

    // Unique and Index
    on(users) { user => declare(user.username defineAs unique, user.email defineAs unique) }
    on(projects) { project => declare(columns(project.userID, project.title) are unique)}

    on(stuffTopics) { stuffTopic => declare(
        columns(stuffTopic.stuffID, stuffTopic.topic) are unique
    )}
    on(stuffProjects) { stuffProjects => declare(
        columns(stuffProjects.stuffID, stuffProjects.projectID) are unique
    )}

    // Foreign Keys
    oneToManyRelation(users, stuffs).via { (user, stuff) => user.id === stuff.userID }
    oneToManyRelation(stuffs, stuffTopics).via { 
        (stuff, stuffTopic) => stuff.id === stuffTopic.stuffID 
    }
    oneToManyRelation(stuffs, stuffProjects).via { 
        (stuff, stuffProject) => stuff.id === stuffProject.stuffID 
    }
    oneToManyRelation(projects, stuffProjects).via { 
        (project, stuffProject) => project.id === stuffProject.projectID 
    }

}    
