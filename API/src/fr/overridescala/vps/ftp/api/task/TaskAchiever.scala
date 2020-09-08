package fr.overridescala.vps.ftp.api.task

trait TaskAchiever {

    def achieve(): Unit

    def preAchieve(): Unit = {

    }

}
