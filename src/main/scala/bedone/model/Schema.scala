package org.bedone.model

import org.squeryl.Schema
import org.squeryl.ForeignKeyDeclaration

import net.liftweb.squerylrecord.RecordTypeMode._

import ManyToMany._

object BeDoneSchema extends Schema 
{
    // Entity 
    val users = table[User]("Users")
    val stuffs = table[Stuff]("Stuffs")

    val maybes = table[Maybe]("Maybes")
    val actions = table[Action]("Actions")
    val scheduleds = table[Scheduled]("Scheduled")
    val delegateds = table[Delegated]("Delegated")

    val projects = table[Project]("Projects")
    val topics = table[Topic]("Topics")
    val contacts = table[Contact]("Contacts")
    val contexts = table[Context]("Contexts")

    val gmailPreferences = table[GMailPreference]("GMailPreference")

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

    val actionContexts = manyToManyRelation(actions, contexts).via[ActionContext](
        (action, context, relation) => 
            (relation.actionID === action.idField, 
             relation.contextID === context.idField)
    )

    // Unique and Index
    on(users) { user => declare(user.username defineAs unique, user.email defineAs unique) }
    on(projects) { project => declare(columns(project.userID, project.title) are unique)}
    on(topics) { topic => declare(columns(topic.userID, topic.title) are unique)}
    on(contexts) { context => declare(columns(context.userID, context.title) are unique)}
    on(contacts) { contact => declare(columns(contact.userID, contact.name) are unique)}

    on(maybes)     { maybe => declare(maybe.idField is primaryKey) }
    on(actions)    { action => declare(action.idField is primaryKey) }
    on(scheduleds) { scheduled => declare(scheduled.idField is primaryKey) }
    on(delegateds) { delegated => declare(delegated.idField is primaryKey) }
    on(gmailPreferences) { preference => declare(preference.idField is primaryKey) }


    // One-to-Many Foreign Keys
    oneToManyRelation(users, stuffs).via { (u, s) => u.id === s.userID }
    oneToManyRelation(users, topics).via { (u, t) => u.id === t.userID }
    oneToManyRelation(users, projects).via { (u, p) => u.id === p.userID }
    oneToManyRelation(users, contexts).via { (u, c) => u.id === c.userID }
    oneToManyRelation(users, gmailPreferences).via { (u, g) => u.id === g.idField }

    oneToManyRelation(stuffs, maybes).via { (s, m) => s.id === m.idField }
    oneToManyRelation(stuffs, actions).via { (s, a) => s.id === a.idField }
    oneToManyRelation(stuffs, scheduleds).via { (st, sc) => st.id === sc.idField }
    oneToManyRelation(stuffs, delegateds).via { (s, d) => s.id === d.idField }

    oneToManyRelation(contacts, delegateds).via { (c, d) => c.id === d.contactID }

    // Foreign Key policy
    override def applyDefaultForeignKeyPolicy(foreignKeyDeclaration: ForeignKeyDeclaration) =
        foreignKeyDeclaration.constrainReference(onDelete cascade)

}

