package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.DataPacket

trait TaskCompleterFactory {

    def getCompleter(initPacket: DataPacket): TaskExecutor

    def putCompleter(completerType: String, supplier: (DataPacket, TasksHandler) => TaskExecutor)

}
