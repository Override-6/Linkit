package fr.`override`.linkit.api.system.event

import fr.`override`.linkit.api.system.event.Event

abstract class EventListener {

    def handleEvent(event: Event[_]): Unit = {
        event match {
            case e: Event[this.type] => e.notifyListener(this)
            case _ => //doNothing
        }
    }


}
