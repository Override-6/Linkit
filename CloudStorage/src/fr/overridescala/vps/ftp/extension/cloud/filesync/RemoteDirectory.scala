package fr.overridescala.vps.ftp.`extension`.cloud.filesync

import java.nio.file.{Files, Path}

import fr.overridescala.vps.ftp.api.packet.PacketChannel
import fr.overridescala.vps.ftp.api.packet.fundamental.DataPacket

class RemoteDirectory(private var path: String)(implicit channel: PacketChannel) {

    def setName(name: String): Unit = {
        val path = Path.of(this.path).resolveSibling(name).toString
        sendOrder("rename", path)
    }

    def delete(): Unit = {
        delete(Path.of(path))
    }

    def sendFile(path: Path): Unit = {
        val in = Files.newInputStream(path)
        val buff = new Array[Byte](Files.size(path).toInt)
        val read = in.read(buff)
        val bytes = buff.slice(0, read)
        channel.sendPacket(DataPacket(s"up:$path", bytes))
        in.close()
    }

    def delete(path: Path): Unit = {
        sendOrder("delete", path.toString)
    }

    private def sendOrder(order: String, affected: String): Unit =
        channel.sendPacket(DataPacket(order, affected))

}
