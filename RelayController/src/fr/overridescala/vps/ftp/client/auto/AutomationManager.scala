package fr.overridescala.vps.ftp.client.auto

import scala.collection.mutable.ListBuffer

class AutomationManager {

    private val automations: ListBuffer[Automation] = ListBuffer.empty
    private var isStarted = false


    def register(automation: Automation): Unit = {
        automations += automation
        if (isStarted)
            startAutomation(automation, automations.size)
    }

    def start(): Unit = {
        isStarted = true
        var counter = 0
        automations.foreach(auto => {
            counter += 1
            startAutomation(auto, counter)
        })
    }

    private def startAutomation(automation: Automation, counter: Int): Unit = {
        val thread = new Thread(() => {
            automation.start()
            automation.stop()
        })
        thread.setName(s"Automation Thread n° $counter")
        thread.start()
    }

}
