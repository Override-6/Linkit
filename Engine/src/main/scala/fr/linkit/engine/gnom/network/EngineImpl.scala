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

package fr.linkit.engine.gnom.network

import fr.linkit.api.gnom.cache.sync.instantiation.New
import fr.linkit.api.gnom.network._
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.internal.mapping.RemoteClassMappings

import java.sql.Timestamp
import scala.collection.mutable

class EngineImpl(override val name: String,
                 network0         : AbstractNetwork) extends Engine {


    private  val identifierSet              = mutable.Set.empty[IdentifierTag]
    private  val groupSet                   = mutable.Set.empty[GroupTag]
    override val reference: EngineReference = new EngineReference(name)
    override val network  : Network         = network0

    private var mappings              : Option[RemoteClassMappings] = None
    private var isMappingsInitializing: Boolean                     = false

    fillInDefaultTags()

    private def fillInDefaultTags(): Unit = {
        identifierSet += IdentifierTag(name)
        groupSet += Everyone
        if (isServer) identifierSet += Server
        else groupSet += Clients
    }

    override def identifiers: Set[IdentifierTag] = identifierSet.toSet

    override def groups: Set[GroupTag] = groupSet.toSet

    override def isTagged(tag: NetworkFriendlyEngineTag): Boolean = tag match {
        case id: IdentifierTag => identifierSet(id)
        case g: GroupTag       => groupSet(g)
        case Current           => isCurrentEngine
    }

    override def addTag(tag: NetworkFriendlyEngineTag): Unit = tag match {
        case id@IdentifierTag(identifier) =>
            if (identifierSet(id)) throw new IllegalTagException(s"'$identifier' is already an identifier of this engine.")
            if (id == Server) throw new IllegalTagException("Cannot add 'Server' tag to a client.")
            if (network.findEngine(id).nonEmpty) throw new IllegalTagException(s"Another engine is already identified with '$identifier'")
            identifierSet += id
        case g@GroupTag(name)             =>
            if (groupSet(g)) throw new IllegalTagException(s"This engine is already present in group '$name'")
            if (g == Clients) throw new IllegalTagException("Cannot add 'Clients' group to server.")
            groupSet += g
    }

    override def removeTag(tag: NetworkFriendlyEngineTag): Unit = tag match {
        case id@IdentifierTag(identifier) =>
            if (identifier == name) throw new IllegalTagException("Cannot remove engine's name from identifiers")
            if (id == Server && isServer) throw new IllegalTagException("Cannot remove 'Server' identifier from identifiers")
            identifierSet -= id
        case g@GroupTag(name)             =>
            if (g == Everyone || (g == Clients && !isServer)) throw new IllegalTagException(s"Cannot remove '$name' group.")
            groupSet -= g
    }


    override def isServer: Boolean = network.serverName == name

    override def isCurrentEngine: Boolean = network.connection.currentIdentifier == name

    def classMappings: Option[RemoteClassMappings] = {
        if (mappings.isEmpty && !isMappingsInitializing) {
            network0.mappingsCache match {
                case None        =>
                case Some(cache) =>
                    isMappingsInitializing = true
                    mappings = {
                        if (isCurrentEngine) {
                            AppLoggers.Mappings.debug(s"Class mappings of this engine has been sent over the network.")
                            Some(cache.syncObject(name.hashCode, New[RemoteClassMappings](name)))
                        } else {
                            val r = cache.findObject(name.hashCode)
                            if (r.isDefined)
                                AppLoggers.Mappings.debug(s"Established connection with class mappings of '$name'.")
                            r
                        }
                    }
                    isMappingsInitializing = false
            }
        }
        mappings
    }

    override def toString: String = if (isCurrentEngine) s"CurrentEngine($name)" else s"DistantEngine($name)"

    override val connectionDate: Timestamp = new Timestamp(System.currentTimeMillis())

    override def getConnectionState: ExternalConnectionState = ExternalConnectionState.CONNECTED

    override def hashCode(): Int = name.hashCode

}
