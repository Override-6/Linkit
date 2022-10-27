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

import fr.linkit.api.gnom.referencing.NetworkObject

import java.sql.Timestamp
/**
 * An engine is an entity that is connected on the network.
 * It can either be the server or any client.
 * */
trait Engine extends NetworkObject[EngineReference] {

    /**
     * this engine's name.
     * the name of an engine is also used as an identifier.
     * thus, two engines can't have the same name, and an engine name is also part of its identifiers (see [[identifiers]])
     * */
    val name: String

    /**
     * the timestamp of the last time this engine connected on the network.
     * */
    val connectionDate: Timestamp

    /**
     * this engine's network
     * */
    val network: Network

    /**
     * A set containing all the identifiers of this engine.
     * IdentifierTags are [[EngineTag]] that can only be used by one engine over all the network.
     * This set contains at least one Identifier Tag which is the user's name.
     * If this engine is the server engine, this set will also contain the [[Server]] identifier tag.
     * */
    def identifiers: Set[IdentifierTag]

    /**
     * A set containing all the groups of this engine.
     * All Engines contains at least one GroupTag which is the [[Everyone]] group tag.
     * If this engine is not the server, and thus is a client, the set will also contain the [[Clients]] group tag
     * */
    def groups: Set[GroupTag]

    /**
     * @return true if this engine is tagged by the given tag
     * */
    def isTagged(tag: EngineTag): Boolean

    /**
     * Adds a new tag for this engine <br>
     * You cannot add two same tags, but an identifier tag and a group tag with the same value can cohabit
     * */
    @throws[IllegalTagException]("if this engine is already referenced by the given tag")
    @throws[IllegalTagException]("if given tag is Server")
    def addTag(tag: EngineTag): Unit

    /**
     * Removes a previously added tag from this engine <br>
     * You cannot remove a default tag, i.e [[Server]], [[Everyone]], [[Clients]] or this engine's name.
     * */
    @throws[IllegalTagException]("if given tag is a default one")
    def removeTag(tag: EngineTag): Unit


    /**
     * This engine's [[fr.linkit.api.gnom.referencing.NetworkObjectReference]] as it is also a [[NetworkObject]]
     * */
    def reference: EngineReference

    /**
     * @return true if this engine is the network's server.
     * */
    def isServer: Boolean = network.serverEngine eq this

    /**
     * Connection state of the current engine
     * */
    @deprecated
    def getConnectionState: ExternalConnectionState

}
