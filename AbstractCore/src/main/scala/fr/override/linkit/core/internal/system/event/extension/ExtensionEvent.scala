package fr.`override`.linkit.core.internal.system.event.`extension`

import fr.`override`.linkit.skull.internal.system.event.extension.ExtensionEventListener
import fr.`override`.linkit.skull.internal.system.event.{Event, EventHook}

trait ExtensionEvent extends Event[ExtensionEventHooks, ExtensionEventListener] {

    protected type ExtensionEventHook = EventHook[ExtensionEventListener, _ <: Event[ExtensionEventHooks, ExtensionEventListener]]

    override def getHooks(category: ExtensionEventHooks): Array[ExtensionEventHook]
}
