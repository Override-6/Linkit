package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.DataPacket

trait DynamicTaskCompleterFactory extends TaskCompleterFactory {

    def putCompleter(completerType: String, supplier: DataPacket => TaskExecutor)

}
