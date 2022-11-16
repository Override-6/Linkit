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
 * A specific kind of tag that is used to make a specific selection over specific engines (see implementations).
 * */
sealed trait SelectionTag extends EngineTag

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
 * A Magic tag is a special type of engine tag where the identified engine may change depending on
 * where you are present on the network.
 * Take a look at its implementation to have a better understanding of magic tags.
 * */

trait MagicTag extends EngineTag

/**
 * a Magic tag that identifies the current engine.
 * */
object Current extends MagicTag with UniqueTag with NetworkFriendlyEngineTag

/**
 * A special tag that selects nobody.<br>
 * Nobody <=> !Everyone
 * */
object Nobody extends MagicTag with NetworkFriendlyEngineTag


/**
 * A Group Tag is a tag that can be attributed to multiple engines in order to define a group, or a set of engine.
 * */
case class GroupTag(name: String) extends NetworkFriendlyEngineTag {
    override def toString: String = s"@$name"
}

/**
 * Includes everyone of the network (server / clients)
 * */
object Everyone extends GroupTag("everyone")

/**
 * Includes clients only.<br>
 * Clients <=> !Server
 * */
object Clients extends GroupTag("clients")

/**
 * Inverses a tag selection.<br>
 * Example: !Server means Clients,
 * <br> !Everyone means Nobody
 * */
final case class NotTag(tag: NetworkFriendlyEngineTag) extends SelectionTag with NetworkFriendlyEngineTag {
    override def toString: String = s"!$tag"
}

/**
 * Union of all given tags.<br>
 * Example: Server U Clients means Everyone,<br>
 * !(NameTag("client1") U NameTag("client2")) means everyone except client1 and client2
 * */
final case class UnionTag(tags: List[NetworkFriendlyEngineTag]) extends SelectionTag with NetworkFriendlyEngineTag {
    override def toString: String = tags.mkString(" U ")
}

final case class IntersectionTag(tags: List[NetworkFriendlyEngineTag]) extends SelectionTag with NetworkFriendlyEngineTag {
    override def toString: String = tags.mkString(" ∩ ")
}

object TagUtils {

    implicit class TagOps(tag: NetworkFriendlyEngineTag) {
        def unary_!(): NotTag = NotTag(tag)

        def -(other: NetworkFriendlyEngineTag): NetworkFriendlyEngineTag = tag I !other

        def U(other: NetworkFriendlyEngineTag): NetworkFriendlyEngineTag = tag match {
            case UnionTag(tags) => other match {
                case UnionTag(tags2) => UnionTag(tags2 ::: tags)
                case tag             => UnionTag(tag :: tags)
            }
            case tag            => UnionTag(other :: tag :: Nil)
        }

        def I(other: NetworkFriendlyEngineTag): NetworkFriendlyEngineTag = tag match {
            case IntersectionTag(tags) => other match {
                case IntersectionTag(tags2) => IntersectionTag(tags2 ::: tags)
                case tag                    => IntersectionTag(tag :: tags)
            }
            case tag                   => IntersectionTag(other :: tag :: Nil)
        }

    }
}

