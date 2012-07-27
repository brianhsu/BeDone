package org.bedone.model

import org.squeryl.Schema
import net.liftweb.squerylrecord.RecordTypeMode._

import ManyToMany._

object BeDoneSchema extends Schema 
{
    // Entity 
    val users = table[User]("Users")
    val stuffs = table[Stuff]("Stuffs")
    val maybes = table[Maybe]("Maybes")
    val actions = table[Action]("Actions")

    val projects = table[Project]("Projects")
    val topics = table[Topic]("Topics")

    // Many to Many Relations
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

    on(actions) { action => declare(action.stuffID is primaryKey) }
    on(maybes) { maybe => declare(maybe.stuffID is primaryKey) }

    // Foreign Keys
    oneToManyRelation(users, stuffs).via { (u, s) => u.id === s.userID }
    oneToManyRelation(users, topics).via { (u, t) => u.id === t.userID }
    oneToManyRelation(users, projects).via { (u, p) => u.id === p.userID }

    oneToManyRelation(stuffs, maybes).via { (s, m) => s.id === m.stuffID }
    oneToManyRelation(stuffs, actions).via { (s, a) => s.id === a.stuffID }
}

