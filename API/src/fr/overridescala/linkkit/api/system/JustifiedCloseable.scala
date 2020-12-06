package fr.overridescala.linkkit.api.system

import fr.overridescala.linkkit.api.system.Reason

trait JustifiedCloseable {

    def close(reason: Reason): Unit

}
