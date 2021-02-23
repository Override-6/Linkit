package fr.`override`.linkit.api.system.event

trait Event[L <: EventListener] {

    def getHooks: Array[_ <: EventHook[L, this.type]]

}
