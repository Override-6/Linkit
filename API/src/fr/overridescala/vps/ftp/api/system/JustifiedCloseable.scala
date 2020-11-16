package fr.overridescala.vps.ftp.api.system

import fr.overridescala.vps.ftp.api.system.Reason

trait JustifiedCloseable {

    def close(reason: Reason): Unit

}
