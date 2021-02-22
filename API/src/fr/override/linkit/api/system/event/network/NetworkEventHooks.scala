package fr.`override`.linkit.api.system.event.network

import fr.`override`.linkit.api.system.event.network.NetworkEvents._
import fr.`override`.linkit.api.system.event.{EventHook, SimpleEventHook}

object NetworkEventHooks {

    def EntityAdded: EventHook[EntityAddedEvent] = SimpleEventHook()

    def EntityRemoved: EventHook[EntityRemovedEvent] = SimpleEventHook()

    def EntityStateChange: EventHook[EntityStateChangeEvent] = SimpleEventHook()

    def EntityEditCurrentProperties: EventHook[RemotePropertyChangeEvent] = SimpleEventHook()

    def EditEntityProperties: EventHook[RemotePropertyChangeEvent] = SimpleEventHook()

    def RemotePrintReceived: EventHook[RemotePrintEvent] = SimpleEventHook()

    def RemotePrintSent: EventHook[RemotePrintEvent] = SimpleEventHook()

}
