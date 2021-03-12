package fr.`override`.linkit.api.system.evente

import scala.collection.mutable.ListBuffer

class EventNotifier {

    private val listeners = ListBuffer[EventListener]()

    def register(listener: EventListener): Unit = listeners += listener

    def unregister(listener: EventListener): Unit = listeners -= listener

    def notifyEvent[C <: EventHookCategory, L <: EventListener](hookCategory: C, event: Event[C, L]): Unit = {
        val eventListeners = listeners
                .filter(_.isInstanceOf[L])
                .map(_.asInstanceOf[L])
        event.getHooks(hookCategory).foreach(_.executeEvent(cast(event), cast(eventListeners.toSeq)))
    }

    def notifyEvent[C <: EventHookCategory, L <: EventListener](event: Event[C, L])(implicit hookCategory: C): Unit = {
        val eventListeners = listeners
                .filter(_.isInstanceOf[L])
                .map(_.asInstanceOf[L])
        event.getHooks(hookCategory).foreach(_.executeEvent(cast(event), cast(eventListeners.toSeq)))
    }

    private def cast[T](any: Any): T = any.asInstanceOf[T] //FIXME remove

}
