package org.bedone.model

import org.squeryl.Schema
import net.liftweb.squerylrecord.RecordTypeMode._

import ManyToMany._

object BeDoneSchema extends Schema 
{
    // Entity 
    val users = table[User]("Users")
    val stuffs = table[Stuff]("Stuffs")
    val references = table[Reference]("Refs")
    val maybes = table[Maybe]("Maybes")

    val projects = table[Project]("Projects")
    val topics = table[Topic]("Topics")

    // Many to Many Relations
    val referenceProjects = manyToManyRelation(references, projects).via[ReferenceProject](
        (reference, project, relation) => 
            (relation.referenceID === reference.idField, 
             relation.projectID === project.idField)
    )

    val referenceTopics = manyToManyRelation(references, topics).via[ReferenceTopic](
        (reference, topic, relation) => 
            (relation.referenceID === reference.idField, 
             relation.topicID === topic.idField)
    )

    val stuffTopics = manyToManyRelation(stuffs, topics).via[StuffTopic](
        (stuff, topic, relation) => 
            (relation.stuffID === stuff.idField, 
             relation.topicID === topic.idField)
    )

    val stuffProjects = manyToManyRelation(stuffs, projects).via[StuffProject](
        (stuff, project, relation) => 
            (relation.stuffID === stuff.idField, 
             relation.projectID === project.idField)
    )

    // Unique and Index
    on(users) { user => declare(user.username defineAs unique, user.email defineAs unique) }
    on(projects) { project => declare(columns(project.userID, project.title) are unique)}
    on(topics) { topic => declare(columns(topic.userID, topic.title) are unique)}

    // Foreign Keys
    oneToManyRelation(users, stuffs).via { (u, s) => u.id === s.userID }
    oneToManyRelation(users, references).via { (u, r) => u.id === r.userID }
    oneToManyRelation(users, topics).via { (u, t) => u.id === t.userID }
    oneToManyRelation(users, projects).via { (u, p) => u.id === p.userID }
}

