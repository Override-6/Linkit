package fr.overridescala.vps.ftp.client.auto

trait Automation {

    def start(): Unit

    protected def cancel(): Unit = {
        val thread = Thread.currentThread()
        println(s"Thread ${thread.getName} closed")
        thread.interrupt()
    }


}
