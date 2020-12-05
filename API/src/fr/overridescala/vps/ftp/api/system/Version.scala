package fr.overridescala.vps.ftp.api.system

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Paths}
import java.util.Date

case class Version(name: String, major: Byte, minor: Byte, patch: Byte, stable: Boolean, buildDate: Date) {

    override def toString: String = s"$name: v$major.$minor.$patch-${if (stable) "stable" else "unstable"} $buildDate"


}

object Version {
    def apply(name: String, major: Byte, minor: Byte, patch: Byte, stable: Boolean): Version = {
        val attributes = Files.readAttributes(Paths.get("/"), classOf[BasicFileAttributes])
        val buildDate = Date.from(attributes.creationTime().toInstant)
        new Version(name, major, minor, patch, stable, buildDate)
    }

}
