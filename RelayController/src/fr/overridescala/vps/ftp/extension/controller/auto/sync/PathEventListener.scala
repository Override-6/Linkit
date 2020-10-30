package fr.overridescala.vps.ftp.`extension`.controller.auto.sync

import java.nio.file.Path

trait PathEventListener {

    def onCreate(affected: Path): Unit

    def onDelete(affected: Path): Unit

    def onModify(affected: Path): Unit

    def onRename(affected: Path, newName: String): Unit

}
