package org.bedone.model

import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.dsl.CompositeKey2
import org.squeryl.KeyedEntity

object ManyToMany
{
    type Key = CompositeKey2[Int,Int]

    class ReferenceProject(val referenceID: Int, val projectID: Int) extends KeyedEntity[Key] {
        def id = compositeKey(referenceID, projectID)
    }

    class ReferenceTopic(val referenceID: Int, val topicID: Int) extends KeyedEntity[Key] {
        def id = compositeKey(referenceID, topicID)
    }

    class StuffTopic(val stuffID: Int, val topicID: Int) extends KeyedEntity[Key] {
        def id = compositeKey(stuffID, topicID)
    }

    class StuffProject(val stuffID: Int, val projectID: Int) extends KeyedEntity[Key] {
        def id = compositeKey(stuffID, projectID)
    }
}
