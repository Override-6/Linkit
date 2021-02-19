package fr.`override`.linkit.api.system.event

trait Event[L <: EventListener] {

    def notifyListener(listener: L): Unit

}
