package fr.`override`.linkit.core.local.system.event.`extension`

import fr.`override`.linkit.api.local.system.event.extension.ExtensionEventListener
import fr.`override`.linkit.api.local.system.event.{Event, EventHook}

trait ExtensionEvent extends Event[ExtensionEventHooks, ExtensionEventListener] {

    protected type ExtensionEventHook = EventHook[ExtensionEventListener, _ <: Event[ExtensionEventHooks, ExtensionEventListener]]

    override def getHooks(category: ExtensionEventHooks): Array[ExtensionEventHook]
}
