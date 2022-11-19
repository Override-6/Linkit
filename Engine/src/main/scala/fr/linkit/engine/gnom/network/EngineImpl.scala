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

import fr.linkit.api.gnom.cache.sync.ConnectedObject
import fr.linkit.api.gnom.cache.sync.instantiation.New
import fr.linkit.api.gnom.network._
import fr.linkit.api.gnom.network.tag._
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.internal.mapping.RemoteClassMappings

import java.sql.Timestamp
import scala.collection.mutable

class EngineImpl private[network](override val name   : String,
                                  override val network: AbstractNetwork) extends Engine {


    private val identifierSet                                       = mutable.Set.empty[IdentifierTag]
    private val groupSet                                            = mutable.Set.empty[Group]
    private var mappings              : Option[RemoteClassMappings] = None
    private var isMappingsInitializing: Boolean                     = false

    override val nameTag  : NameTag         = NameTag(name)
    override val reference: EngineReference = new EngineReference(nameTag)

    fillInDefaultTags()


    private def fillInDefaultTags(): Unit = {
        groupSet += Everyone
        if (isServer) identifierSet += Server
        else groupSet += Clients
    }

    override def identifiers: Set[IdentifierTag] = identifierSet.toSet

    override def groups: Set[Group] = groupSet.toSet


    override def isIncluded(tag: NetworkFriendlyEngineTag): Boolean = tag match {
        case Nobody            => false
        case Everyone          => true
        case n: NameTag        => n == nameTag
        case Current           => isCurrentEngine
        case id: IdentifierTag => identifierSet(id)
        case g: Group          => groupSet(g)
    }

    override def isIncluded(tag: TagSelection[NetworkFriendlyEngineTag]): Boolean = tag match {
        case Select(tag)        => isIncluded(tag)
        case Not(tag)           => !isIncluded(tag)
        case Union(a, b)        => isIncluded(a) || isIncluded(b)
        case Intersection(a, b) => isIncluded(a) && isIncluded(b)
    }

    override def addTag(tag: NetworkFriendlyEngineTag): Unit = tag match {
        case id@IdentifierTag(identifier) =>
            if (identifierSet(id))
                throw new IllegalTagException(s"'$identifier' is already an identifier of this engine.")
            if (id == Server)
                throw new IllegalTagException("Cannot add 'Server' tag to a client.")
            if (network.getEngine(id).nonEmpty)
                throw new IllegalTagException(s"Another engine is already identified with '$identifier'")

            identifierSet += id
        case g@Group(name)                =>
            if (groupSet(g))
                throw new IllegalTagException(s"This engine is already present in group '$name'")
            if (g == Clients)
                throw new IllegalTagException("Cannot add 'Clients' group to server.")

            groupSet += g
        case Current | Nobody             =>
            throw new IllegalTagException(s"Current / Nobody tags are not allowed for attribution")
    }

    override def removeTag(tag: NetworkFriendlyEngineTag): Unit = tag match {
        case id@IdentifierTag(identifier) =>
            if (identifier == name)
                throw new IllegalTagException("Cannot remove engine's name from identifiers")
            if (id == Server && isServer)
                throw new IllegalTagException("Cannot remove 'Server' identifier from identifiers")
            else identifierSet -= id
        case g@Group(name)                =>
            if (g == Everyone || (g == Clients && !isServer))
                throw new IllegalTagException(s"Cannot remove '$name' group.")
            else groupSet -= g
        case Current | Nobody             =>
            throw new IllegalTagException(s"Cannot remove Current / Nobody tags")
    }

    import network._

    override def isServer: Boolean = nameTag <=> Server

    override def isCurrentEngine: Boolean = network.connection.currentName == name

    def classMappings: Option[RemoteClassMappings] = {
        if (this.isInstanceOf[ConnectedObject[_]] && mappings.isEmpty && !isMappingsInitializing) {
            network.mappingsCache match {
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
