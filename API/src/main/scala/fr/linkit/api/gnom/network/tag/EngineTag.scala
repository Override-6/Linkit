/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.gnom.network.tag

import fr.linkit.api.gnom.cache.sync.contract.OwnerEngine
import fr.linkit.api.gnom.network.Network

/**
 * An Engine tag is a tag that represents an engine or a group of engine
 * over the network.
 * */
sealed trait EngineTag

/**
 * Network Friendly Tags are any engine tag that can be used and interpreted by the [[Network]] trait. <br>
 * All Group and Identifier tags extends [[NetworkFriendlyEngineTag]] because they are understandable by the [[Network]],
 * but magic tags, as they are magic, cannot all be understood by the [[Network]].<br>
 * For example, the [[Current]] magic tag is a network-friendly tag because the network knows how to handle the special case
 * of the 'Current' tag. <br>
 * [[OwnerEngine]] is a magic tag coming from the connected objects system,
 * and thus is not a network-friendly tag because using this tag at the network level means nothing.
 * */
sealed trait NetworkFriendlyEngineTag extends EngineTag

/**
 * a unique tag is a tag that identifies only one engine
 * */
trait UniqueTag extends EngineTag

/**
 * an identifier tag is a tag that can only be attributed to one engine.
 * Identifier tags are unique and two engine cannot have the same identifier tag.
 * An engine can have multiple identifier tags.
 * */
case class IdentifierTag(id: String) extends UniqueTag with NetworkFriendlyEngineTag {
    override def toString: String = s"#$id"
}

/**
 * a name tag is a tag that can only be attributed to one engine. NameTags are unique
 * and two engines cannot have the same name tag.
 * An engine can have only one name tag which is it's original network's name.
 * */
case class NameTag(name: String) extends UniqueTag with NetworkFriendlyEngineTag {
    override def toString: String = name
}

object Server extends IdentifierTag("server")



/**
 * a Magic tag that identifies the current engine.<br>
 * If this tag is sent to an engine, it magically transforms itself to the name tag of the sender
 * */
case object Current extends UniqueTag with NetworkFriendlyEngineTag

/**
 * A special tag that selects nobody.<br>
 * Nobody <=> !Everyone
 * */
case object Nobody extends NetworkFriendlyEngineTag


/**
 * A Group Tag is a tag that can be attributed to multiple engines in order to define a group, or a set of engine.
 * */
case class Group(name: String) extends NetworkFriendlyEngineTag {
    override def toString: String = s"@$name"
}

/**
 * Includes everyone of the network (server / clients)
 * */
object Everyone extends Group("everyone")

/**
 * Includes clients only.<br>
 * Clients <=> !Server
 * */
object Clients extends Group("clients")


