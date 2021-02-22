package fr.`override`.linkit.prototypes

abstract class EventListener {

    def onX(action: => Unit): Unit

    def onY(action: => Unit): Unit

    def onZ(action: => Unit): Unit

}
