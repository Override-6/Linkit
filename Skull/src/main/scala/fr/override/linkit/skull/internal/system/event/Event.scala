package fr.`override`.linkit.skull.internal.system.event

trait Event[C <: EventHookCategory, L <: EventListener] {

    def getHooks(category: C): Array[_ <: EventHook[_ <: L, _ <: Event[_ <: C, _ <: L]]]

}
