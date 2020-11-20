package fr.overridescala.vps.ftp.`extension`.cloud.filesync

import java.nio.file.Path

import fr.overridescala.vps.ftp.api.packet.PacketChannel

class FolderListener(channel: PacketChannel, root: String) {

    def onRename(oldPath: Path, newPath: Path): Unit = {

    }

    def onDelete(affected: Path): Unit = {

    }

    def onCreate(affected: Path): Unit = {

    }

    def onChange(affected: Path): Unit = {

    }

}
