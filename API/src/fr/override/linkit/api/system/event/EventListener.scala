package fr.`override`.linkit.api.system.event

abstract class EventListener {

    def handleEvent(event: Event[_]): Unit = {
        event match {
            case e: Event[this.type] => e.notifyListener(this)
            case _ =>
        }
    }

}
