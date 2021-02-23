package fr.`override`.linkit.api.system.event.extension

import fr.`override`.linkit.api.system.event.{Event, EventHook}
import fr.`override`.linkit.api.system.event.`extension`.ExtensionEventListener

trait ExtensionEvent extends Event[ExtensionEventListener] {

    override def getHooks: Array[EventHook[ExtensionEventListener, this.type]]

}
