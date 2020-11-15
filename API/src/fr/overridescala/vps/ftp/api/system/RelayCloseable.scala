package fr.overridescala.vps.ftp.api.system

import fr.overridescala.vps.ftp.api.system.Reason

trait RelayCloseable {

    def close(reason: Reason): Unit

}
