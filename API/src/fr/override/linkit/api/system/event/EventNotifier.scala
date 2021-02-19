package fr.`override`.linkit.api.system.event

import scala.collection.mutable.ListBuffer

class EventNotifier {

    private val listeners = ListBuffer[EventListener]()

    def register(listener: EventListener): Unit = listeners += listener

    def unregister(listener: EventListener): Unit = listeners -= listener

    def notifyEvent(event: Event[_]): Unit = {
        for (listener <- listeners) {
            listener.handleEvent(event)
        }
    }

}
