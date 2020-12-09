package fr.overridescala.linkkit.api.system

trait JustifiedCloseable {

    def close(reason: Reason): Unit

}
