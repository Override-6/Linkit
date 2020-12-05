package fr.overridescala.vps.ftp.`extension`.controller.auto

trait Automation {

    def start(): Unit

    protected[auto] def stop(): Unit = {
        val thread = Thread.currentThread()
        println(s"Thread ${thread.getName} closed")
        thread.interrupt()
    }


}
