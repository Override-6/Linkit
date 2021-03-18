package fr.`override`.linkit.core.local.system.event

import fr.`override`.linkit.api.local.system.event.{Event, EventHookCategory, EventListener, EventNotifier}

import scala.collection.mutable.ListBuffer

class SimpleEventNotifier extends EventNotifier {

    private val listeners = ListBuffer[EventListener]()

    override def register(listener: EventListener): Unit = listeners += listener

    override def unregister(listener: EventListener): Unit = listeners -= listener

    override def notifyEvent[C <: EventHookCategory, L <: EventListener](hookCategory: C, event: Event[C, L]): Unit = {
        val eventListeners = listeners
                .filter(_.isInstanceOf[L])
                .map(_.asInstanceOf[L])
        event.getHooks(hookCategory).foreach(_.executeEvent(cast(event), cast(eventListeners.toSeq)))
    }

    override def notifyEvent[C <: EventHookCategory, L <: EventListener](event: Event[C, L])(implicit hookCategory: C): Unit = {
        val eventListeners = listeners
                .filter(_.isInstanceOf[L])
                .map(_.asInstanceOf[L])
        event.getHooks(hookCategory).foreach(_.executeEvent(cast(event), cast(eventListeners.toSeq)))
    }

    private def cast[T](any: Any): T = any.asInstanceOf[T] //FIXME remove

}
