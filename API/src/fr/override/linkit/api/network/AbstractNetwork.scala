/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.api.network

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.network.cache.SharedCacheHandler
import fr.`override`.linkit.api.network.cache.collection.{BoundedCollection, CollectionModification, SharedCollection}
import fr.`override`.linkit.api.packet.fundamental.RefPacket.ObjectPacket
import fr.`override`.linkit.api.packet.traffic.ChannelScope
import fr.`override`.linkit.api.packet.traffic.channel.CommunicationPacketChannel

abstract class AbstractNetwork(relay: Relay) extends Network {

    override val globalCache: SharedCacheHandler = SharedCacheHandler.create("Global Shared Cache", Relay.ServerIdentifier)(relay.traffic)
    relay.packetTranslator.completeInitialisation(globalCache)

    override val selfEntity: SelfNetworkEntity = new SelfNetworkEntity(relay)

    protected val sharedIdentifiers: SharedCollection[String] = globalCache
            .get(3, SharedCollection.set[String])

    protected val entities: BoundedCollection.Immutable[NetworkEntity]
    private val communicator = relay.createInjectable(9, ChannelScope.broadcast, CommunicationPacketChannel.providable)

    override def listEntities: List[NetworkEntity] = entities.to(List)

    override def addOnEntityAdded(action: NetworkEntity => Unit): Unit = {
        entities.addListener((modKind, _, entity) => {
            if (modKind == CollectionModification.ADD && entity.isDefined)
                action(entity.orNull)
        })
    }

    override def getEntity(identifier: String): Option[NetworkEntity] = {
        if (entities != null)
            entities.find(_.identifier == identifier)
        else None
    }

    //Will replace the entity if the identifier is already present in the network's entities cache.
    def createEntity(identifier: String): NetworkEntity = {
        if (identifier == relay.identifier) {
            return selfEntity
        }

        val channel = communicator.subInjectable(Array(identifier), CommunicationPacketChannel.providable, true)
        val ent = createRelayEntity(identifier, channel)
        ent
    }

    def createRelayEntity(identifier: String, communicationChannel: CommunicationPacketChannel): NetworkEntity

    communicator.addRequestListener((packet, coords) => {
        val sender = coords.senderID
        packet match {
            case ObjectPacket(("getProp", name: String)) =>
                val prop = relay.properties.get(name).orNull
                communicator.sendResponse(ObjectPacket(prop), sender)

            case ObjectPacket(("setProp", name: String, value)) =>
                relay.properties.putProperty(name, value)
        }
    })

}

