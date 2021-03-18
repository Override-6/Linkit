package fr.`override`.linkit.api.local.system.event

trait EventNotifier {

    def register(listener: EventListener): Unit

    def unregister(listener: EventListener): Unit

    def notifyEvent[C <: EventHookCategory, L <: EventListener](hookCategory: C, event: Event[C, L]): Unit

    def notifyEvent[C <: EventHookCategory, L <: EventListener](event: Event[C, L])(implicit hookCategory: C): Unit

}
