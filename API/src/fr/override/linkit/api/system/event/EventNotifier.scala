package fr.`override`.linkit.api.system.event

import scala.collection.mutable.ListBuffer

class EventNotifier {

    private val listeners = ListBuffer[EventListener]()

    def register(listener: EventListener): Unit = listeners += listener

    def unregister(listener: EventListener): Unit = listeners -= listener

    def notifyEvent[L <: EventListener](event: Event[L]): Unit = {
        val eventListeners = listeners
            .filter(_.isInstanceOf[L])
            .map(_.asInstanceOf[L])
        event.getHooks.foreach(_.executeEvent(event, eventListeners.toSeq))
    }

}
