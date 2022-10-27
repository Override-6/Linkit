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

package fr.linkit.api.gnom.network

/**
 * An Engine tag is a tag that represents an engine or a group of engine
 * over the network.
 * */
sealed trait EngineTag

/**
 * an identifier tag is a tag that can only be attributed to one engine.
 * Identifier tags are unique and two engine cannot have the same identifier tag.
 * An engine can have multiple identifier tags.
 * */
case class IdentifierTag(identifier: String) extends EngineTag

object Server extends IdentifierTag("server")

/**
 * A Group Tag is a tag that can be attributed to multiple engines in order to define a group, or a set of engine.
 * */
case class GroupTag(name: String) extends EngineTag

object Everyone extends GroupTag("everyone")
object Clients extends GroupTag("clients")