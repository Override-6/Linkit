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

package fr.`override`.linkit.api.system.evente.relay

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
                //TODO case CONNECTING => category.connecting
                //TODO case DISCONNECTED => category.disconnected
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
