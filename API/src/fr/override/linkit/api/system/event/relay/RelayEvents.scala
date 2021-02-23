package fr.`override`.linkit.api.system.event.relay

import fr.`override`.linkit.api.system.{RelayState, SystemOrder}

object RelayEvents {

    case class RelayStateEvent(state: RelayState) extends RelayEvent {
        override def getHooks(category: RelayEventHooks): Array[RelayEventHook] = {
            import RelayState._
            val hook = state match {
                case CRASHED => category.crashed
                case CLOSED => category.closed
                case ENABLED => category.ready
                case ENABLING => category.start
                case CONNECTING => category.connecting
                case DISCONNECTED => category.disconnected
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

    def stateChange(state: RelayState): RelayStateEvent = {
        !!(RelayStateEvent(state))
    }

    def orderReceived(order: SystemOrder): OrderReceivedEvent = OrderReceivedEvent(order)

    private def !![A](any: Any): A = any.asInstanceOf[A]

}
