/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.core.local.system.event.network

import fr.linkit.api.connection.network.{ExternalConnectionState, NetworkEntity}

object NetworkEvents {

    case class EntityAddedEvent(override val entity: NetworkEntity) extends NetworkEvent {

        override def getHooks(category: NetworkEventHooks): Array[NetworkEventHook] = !!(Array(category.entityAdded))
    }

    case class EntityRemovedEvent(override val entity: NetworkEntity) extends NetworkEvent {

        override def getHooks(category: NetworkEventHooks): Array[NetworkEventHook] = !!(Array(category.entityRemoved))
    }

    case class EntityStateChangeEvent(override val entity: NetworkEntity,
                                      newState: ExternalConnectionState,
                                      oldState: ExternalConnectionState) extends NetworkEvent {

        override def getHooks(category: NetworkEventHooks): Array[NetworkEventHook] = !!(Array(category.entityStateChange))
    }

    case class RemotePropertyChangeEvent(override val entity: NetworkEntity,
                                         name: String,
                                         newProperty: Serializable,
                                         oldProperty: Any,
                                         private val currentChanged: Boolean) extends NetworkEvent {

        override def getHooks(category: NetworkEventHooks): Array[NetworkEventHook] = {
            if (currentChanged)
                !!(Array(category.entityEditCurrentProperties))
            else
                !!(Array(category.editEntityProperties))
        }
    }

    case class RemotePrintEvent(override val entity: NetworkEntity,
                                print: String,
                                private val received: Boolean) extends NetworkEvent {

        override def getHooks(category: NetworkEventHooks): Array[NetworkEventHook] = {
            if (received)
                !!(Array(category.remotePrintReceived))
            else
                !!(Array(category.remotePrintSent))
        }
    }

    def entityAdded(entity: NetworkEntity): EntityAddedEvent = EntityAddedEvent(entity)

    def entityRemoved(entity: NetworkEntity): EntityRemovedEvent = EntityRemovedEvent(entity)

    def entityStateChange(entity: NetworkEntity,
                          newState: ExternalConnectionState, oldState: ExternalConnectionState): EntityStateChangeEvent = {
        EntityStateChangeEvent(entity, newState, oldState)
    }

    def remotelyCurrentPropertyChange(entity: NetworkEntity,
                                      name: String,
                                      newProperty: Serializable,
                                      oldProperty: Any): RemotePropertyChangeEvent = {
        RemotePropertyChangeEvent(entity, name, newProperty, oldProperty, true)
    }

    def remotePropertyChange(entity: NetworkEntity,
                             name: String,
                             newProperty: Serializable,
                             oldProperty: Serializable): RemotePropertyChangeEvent = {
        RemotePropertyChangeEvent(entity, name, newProperty, oldProperty, false)
    }

    def remotePrintSentEvent(entity: NetworkEntity, print: String): RemotePrintEvent = {
        RemotePrintEvent(entity, print, false)
    }

    def remotePrintReceivedEvent(entity: NetworkEntity, print: String): RemotePrintEvent = {
        RemotePrintEvent(entity, print, true)
    }

    private def !![A](any: Any): A = any.asInstanceOf[A] //FIXME REMOVE THIS

}
