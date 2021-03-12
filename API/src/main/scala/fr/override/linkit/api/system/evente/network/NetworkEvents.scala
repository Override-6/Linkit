package fr.`override`.linkit.api.system.evente.network

import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}

object NetworkEvents {

    case class EntityAddedEvent(override val entity: NetworkEntity) extends NetworkEvent {
        override def getHooks(category: NetworkEventHooks): Array[NetworkEventHook] = !!(Array(category.entityAdded))
    }

    case class EntityRemovedEvent(override val entity: NetworkEntity) extends NetworkEvent {
        override def getHooks(category: NetworkEventHooks): Array[NetworkEventHook] = !!(Array(category.entityRemoved))
    }

    case class EntityStateChangeEvent(override val entity: NetworkEntity,
                                      newState: ConnectionState,
                                      oldState: ConnectionState) extends NetworkEvent {
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
                          newState: ConnectionState, oldState: ConnectionState): EntityStateChangeEvent = {
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
