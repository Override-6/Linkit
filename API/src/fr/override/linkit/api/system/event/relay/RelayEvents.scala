package fr.`override`.linkit.api.system.event.relay

import fr.`override`.linkit.api.system.event.relay.RelayEventHooks._
import fr.`override`.linkit.api.system.{RelayState, SystemOrder}

object RelayEvents {

    case class RelayStateEvent(state: RelayState) extends RelayEvent {
        override def getHooks: Array[RelayEventHook] = {
            import RelayState._
            val hook = state match {
                case CRASHED => Crashed
                case CLOSED => Closed
                case ENABLED => Ready
                case ENABLING => Start
                case CONNECTING => Connecting
                case DISCONNECTED => Disconnected
                case _ => throw new IllegalArgumentException(s"$state is not a state that can trigger an event.")
            }
            Array(StateChange, hook)
        }
    }

    case class OrderReceivedEvent(order: SystemOrder) extends RelayEvent {
        override def getHooks: Array[RelayEventHook] = {
            Array(OrderReceived)
        }
    }

    def stateChange(state: RelayState): RelayStateEvent = {
        RelayStateEvent(state)
    }

    def orderReceived(order: SystemOrder): OrderReceivedEvent = OrderReceivedEvent(order)


}
