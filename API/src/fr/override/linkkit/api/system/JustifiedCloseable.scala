package fr.`override`.linkkit.api.system

trait JustifiedCloseable {

    def close(reason: Reason): Unit

}
