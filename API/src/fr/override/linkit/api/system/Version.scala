package fr.`override`.linkit.api.system

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Paths}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

case class Version(name: String, major: Byte, minor: Byte, patch: Byte, stable: Boolean, buildDate: Date) {

      override def toString: String = {
            val dateFormat = new SimpleDateFormat("mm/dd/yyyy-hh:mm:ss").format(buildDate)
            s"$name: v$major.$minor.$patch-${if (stable) "stable" else "unstable"} $dateFormat"
      }


}

object Version {
      def apply(name: String, major: Byte, minor: Byte, patch: Byte, stable: Boolean): Version = {
            val attributes = Files.readAttributes(Paths.get("/"), classOf[BasicFileAttributes])
            val buildDate = Date.from(attributes.creationTime().toInstant)
            new Version(name, major, minor, patch, stable, buildDate)
      }


      def apply(name: String, version: String, stable: Boolean): Version = {
            val args = version.split('.')

            if (args.length != 3)
                  throw new IllegalArgumentException(s"version '$version' is incompatible")

            val major = args(0).toByte
            val minor = args(1).toByte
            val patch = args(2).toByte
            Version(name, major, minor, patch, stable)
      }
}
