package fr.`override`.linkit.core.local.system.event.relay

import fr.`override`.linkit.api.connection.network.ConnectionState
import fr.`override`.linkit.api.local.system.RelayState
import fr.`override`.linkit.core.local.system.SystemOrder

object RelayEvents {

    case class RelayStateEvent(state: RelayState) extends RelayEvent {
        override def getHooks(category: RelayEventHooks): Array[RelayEventHook] = {
            import RelayState._
            val hook = state match {
                case CRASHED => category.crashed
                case CLOSED => category.closed
                case ENABLED => category.ready
                case ENABLING => category.start
                case _ => throw new IllegalArgumentException(s"$state is not a state that can trigger an event.")
            }
            !!(Array(category.stateChange, hook))
        }
    }

    case class OrderReceivedEvent(order: SystemOrder) extends RelayEvent {
        override def getHooks(category: RelayEventHooks): Array[RelayEventHook] = {
            !!(Array(category.orderReceived))
        }
    }

    case class ConnectionStateEvent(connectionIdentifier: String, state: ConnectionState) extends RelayEvent {
        override def getHooks(category: RelayEventHooks): Array[RelayEventHook] = {
            import ConnectionState._
            val hook = state match {
                case CONNECTED => category.connected
                case CONNECTING => category.connecting
                case DISCONNECTED => category.disconnected
                case CLOSED => category.connectionClosed
            }
            Array(category.connectionStateChange, hook)
        }
    }

    def stateChange(state: RelayState): RelayStateEvent = {
        RelayStateEvent(state)
    }

    def connectionStateChange(connectionIdentifier: String, state: ConnectionState): ConnectionStateEvent = {
        ConnectionStateEvent(connectionIdentifier, state)
    }

    def orderReceived(order: SystemOrder): OrderReceivedEvent = OrderReceivedEvent(order)

    private def !![A](any: Any): A = any.asInstanceOf[A]

}
