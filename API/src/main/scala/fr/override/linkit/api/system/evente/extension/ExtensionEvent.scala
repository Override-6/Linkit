package fr.`override`.linkit.api.system.evente.extension

import fr.`override`.linkit.api.system.evente.extension.ExtensionEventListener
import fr.`override`.linkit.api.system.evente.{Event, EventHook}

trait ExtensionEvent extends Event[ExtensionEventHooks, ExtensionEventListener] {

    protected type ExtensionEventHook = EventHook[ExtensionEventListener, _ <: Event[ExtensionEventHooks, ExtensionEventListener]]

    override def getHooks(category: ExtensionEventHooks): Array[ExtensionEventHook]
}
