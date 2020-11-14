package fr.overridescala.vps.ftp.api

trait RelayCloseable {

    def close(reason: Reason): Unit

}
