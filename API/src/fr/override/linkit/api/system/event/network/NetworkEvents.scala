package fr.`override`.linkit.api.system.event.network

import fr.`override`.linkit.api.network.{ConnectionState, NetworkEntity}

object NetworkEvents {

    case class EntityAddedEvent(override val entity: NetworkEntity) extends NetworkEvent {
        override def notifyListener(listener: NetworkEventListener): Unit = listener.onEntityAdded(this)
    }

    case class EntityRemovedEvent(override val entity: NetworkEntity) extends NetworkEvent {
        override def notifyListener(listener: NetworkEventListener): Unit = listener.onEntityRemoved(this)
    }

    case class EntityStateChangeEvent(override val entity: NetworkEntity,
                                      newState: ConnectionState,
                                      oldState: ConnectionState) extends NetworkEvent {
        override def notifyListener(listener: NetworkEventListener): Unit = listener.onEntityStateChange(this)
    }

    case class RemotePropertyChangeEvent(override val entity: NetworkEntity,
                                         name: String,
                                         newProperty: Serializable,
                                         oldProperty: Serializable,
                                         private val currentChanged: Boolean) extends NetworkEvent {
        override def notifyListener(listener: NetworkEventListener): Unit = {
            if (currentChanged)
                listener.onEntityEditCurrentProperties(this)
            else
                listener.onEditEntityProperties(this)
        }
    }

    case class RemotePrintEvent(override val entity: NetworkEntity,
                                print: String,
                                private val received: Boolean) extends NetworkEvent {
        override def notifyListener(listener: NetworkEventListener): Unit = {
            if (received)
                listener.onRemotePrintReceived(this)
            else
                listener.onRemotePrintSent(this)
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
                                      oldProperty: Serializable): RemotePropertyChangeEvent = {
        RemotePropertyChangeEvent(entity, name, newProperty, oldProperty, true)
    }

    def remotelyChangeProperty(entity: NetworkEntity,
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

}
