package org.bedone.model

import org.squeryl.Schema
import net.liftweb.squerylrecord.RecordTypeMode._

object BeDoneSchema extends Schema {
    
    // Entity 
    val users = table[User]("users")
    val stuffs = table[Stuff]("stuffs")
    val references = table[Reference]("refs")

    val projects = table[Project]("projects")
    val topics = table[Topic]("topics")

    // Relations
    val stuffTopics = table[StuffTopic]("stuff_topics")
    val stuffProjects = table[StuffProject]("stuff_projects")
    val referenceTopics = table[ReferenceTopic]("reference_topic")

    // Unique and Index
    on(users) { user => declare(user.username defineAs unique, user.email defineAs unique) }
    on(projects) { project => declare(columns(project.userID, project.title) are unique)}
    on(topics) { topic => declare(columns(topic.userID, topic.title) are unique)}

    on(stuffTopics) { stuffTopic => declare(
        columns(stuffTopic.stuffID, stuffTopic.topicID) are unique
    )}

    on(stuffProjects) { stuffProjects => declare(
        columns(stuffProjects.stuffID, stuffProjects.projectID) are unique
    )}

    on(referenceTopics) { referenceTopic => declare(
        columns(referenceTopic.referenceID, referenceTopic.topicID) are unique
    )}

    // Foreign Keys
    oneToManyRelation(users, stuffs).via { (user, stuff) => user.id === stuff.userID }
    oneToManyRelation(users, references).via { (user, reference) => 
        user.id === reference.userID 
    }
    oneToManyRelation(users, topics).via { (user, topic) => user.id === topic.userID }
    oneToManyRelation(users, projects).via { (user, project) => user.id === project.userID }

    oneToManyRelation(stuffs, stuffTopics).via { 
        (stuff, stuffTopic) => stuff.id === stuffTopic.stuffID 
    }
    oneToManyRelation(topics, stuffTopics).via { 
        (topic, stuffTopic) => topic.id === stuffTopic.topicID 
    }

    oneToManyRelation(stuffs, stuffProjects).via { 
        (stuff, stuffProject) => stuff.id === stuffProject.stuffID 
    }
    oneToManyRelation(projects, stuffProjects).via { 
        (project, stuffProject) => project.id === stuffProject.projectID 
    }

    oneToManyRelation(references, referenceTopics).via { 
        (reference, referenceTopic) => reference.id === referenceTopic.referenceID 
    }
    oneToManyRelation(topics, referenceTopics).via { 
        (topic, referenceTopic) => topic.id === referenceTopic.topicID 
    }

}    
