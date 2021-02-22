package fr.`override`.linkit.api.system.event

trait Event[L <: EventListener] {

    def getHooks: Array[EventHook[this.type]]

    def notifyListener(listener: L): Unit
}
