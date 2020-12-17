package fr.`override`.linkit.api.system

trait JustifiedCloseable {

    def close(reason: Reason): Unit

}
