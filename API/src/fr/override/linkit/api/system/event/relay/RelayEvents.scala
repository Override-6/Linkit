package fr.`override`.linkit.api.system.event.relay

import fr.`override`.linkit.api.system.{RelayState, SystemOrder}

object RelayEvents {

    case class RelayStateEvent(state: RelayState) extends RelayEvent {
        override def notifyHooks(listener: RelayEventListener): Unit = {
            import RelayState._
            state match {
                case CRASHED => listener.onCrashed()
                case CLOSED => listener.onClosed()
                case ENABLED => listener.onReady()
                case ENABLING => listener.onStart()
                case CONNECTING => listener.onConnecting()
                case DISCONNECTED => listener.onDisconnected()
                case _ => throw new IllegalArgumentException(s"$state is not a state that can trigger an event.")
            }
            listener.onStateChange(this)

        }
    }

    case class OrderReceivedEvent(order: SystemOrder) extends RelayEvent {
        override def notifyHooks(listener: RelayEventListener): Unit = {
            listener.onOrderReceived(this)
        }
    }

    def stateChange(state: RelayState): RelayStateEvent = {
        RelayStateEvent(state)
    }

    def orderReceived(order: SystemOrder): OrderReceivedEvent = OrderReceivedEvent(order)



}
