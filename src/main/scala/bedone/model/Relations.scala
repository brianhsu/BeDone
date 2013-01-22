package org.bedone.model

import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.dsl.CompositeKey2
import org.squeryl.KeyedEntity

object ManyToMany
{
    type Key = CompositeKey2[Int,Int]

    class StuffTopic(val stuffID: Int, val topicID: Int) extends KeyedEntity[Key] {
        def id = compositeKey(stuffID, topicID)
    }

    class StuffProject(val stuffID: Int, val projectID: Int) extends KeyedEntity[Key] {
        def id = compositeKey(stuffID, projectID)
    }

    class ActionContext(val actionID: Int, val contextID: Int) extends KeyedEntity[Key] {
        def id = compositeKey(actionID, contextID)
    }
}
