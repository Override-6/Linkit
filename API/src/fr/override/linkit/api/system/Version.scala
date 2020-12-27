package fr.`override`.linkit.api.system

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Paths}
import java.text.SimpleDateFormat
import java.util.Date

import fr.`override`.linkit.api.system.Version.DATE_FORMAT

case class Version(name: String, major: Byte, minor: Byte, patch: Byte, stable: Boolean, buildDate: Date) {

    override def toString: String = {
        val dateFormat = DATE_FORMAT.format(buildDate)
        s"$name: v$major.$minor.$patch-${if (stable) "stable" else "unstable"} $dateFormat"
    }


}

object Version {

    private val PATTERN = "name: x.y.z-stable|unstable buildDate"
    private val DATE_FORMAT = new SimpleDateFormat("mm/dd/yyyy-hh:mm:ss")

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


    @throws[IllegalArgumentException]("If the version object could not be built from this string")
    def fromString(str: String): Version = {
        val expressions = str.split(" ")
        checkPattern(expressions.length == 3)

        val name = expressions(0).takeWhile(':' !=)
        val (major, minor, patch, stable) = getSemVer(expressions(1))
        val buildDate = DATE_FORMAT.parse(expressions(2))

        Version(name, major, minor, patch, stable, buildDate)
    }

    private def getSemVer(str: String): (Byte, Byte, Byte, Boolean) = {
        checkPattern(str.startsWith("v") || str.endsWith("-stable") || str.endsWith("-unstable"))

        val stable = str.endsWith("stable")
        val versions = str.slice(1, str.indexOf('-')).split('.')

        checkPattern(versions.length == 3)

        val (major, minor, patch) = try {

            val maj = versions(0).toByte
            val min = versions(1).toByte
            val pat = versions(2).toByte

            (maj, min, pat)
        } catch {
            case e: NumberFormatException =>
                throw new IllegalArgumentException("Must follow this pattern : " + PATTERN, e)
        }

        (major, minor, patch, stable)
    }

    private def checkPattern(success: Boolean, cause: Throwable = null): Unit = {
        if (!success)
            throw new IllegalArgumentException("Must follow this pattern : " + PATTERN, cause)
    }
}
