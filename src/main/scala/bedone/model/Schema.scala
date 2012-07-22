package org.bedone.model

import org.squeryl.Schema

object BeDoneSchema extends Schema {
    val users = table[User]("users")
}
