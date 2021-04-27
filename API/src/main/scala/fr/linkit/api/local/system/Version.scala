/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.local.system

import fr.linkit.api.connection.packet.serialization.StringRepresentable

case class Version(name: String, major: Byte, minor: Byte, patch: Byte, stable: Boolean) extends Serializable {

    override def toString: String = {
        s"$name: v$major.$minor.$patch-${if (stable) "stable" else "unstable"}"
    }

}

object Version extends StringRepresentable[Version] {

    val Unknown: Version = Version("Unknown", "0.0.0", false)

    private val PATTERN = "name: vX.Y.Z-<stable|unstable>"

    override def getRepresentation(t: Version): String = t.toString

    def apply(str: String): Version = fromRepresentation(str)

    @throws[IllegalArgumentException]("If the Version object could not be built from this string")
    override def fromRepresentation(expr: String): Version = {
        implicit val str: String = expr
        val expressions = str.split(' ')
        checkPattern(expressions.length == 2)

        val name                          = expressions(0).dropRight(1)
        val (major, minor, patch, stable) = getSemVer(expressions(1))

        Version(name, major, minor, patch, stable)
    }

    def apply(name: String, code: String, stable: Boolean): Version = {
        val args = code.split('.')

        if (args.length != 3)
            throw new IllegalArgumentException(s"version '$code' is incompatible")

        val major = args(0).toByte
        val minor = args(1).toByte
        val patch = args(2).toByte
        Version(name, major, minor, patch, stable)
    }

    private def getSemVer(str: String)(implicit version: String): (Byte, Byte, Byte, Boolean) = {
        checkPattern(str.startsWith("v") || str.endsWith("-stable") || str.endsWith("-unstable"))

        val stable   = str.endsWith("stable")
        val versions = str.slice(1, str.indexOf('-')).split('.')

        checkPattern(versions.length == 3)

        val (major, minor, patch) = try {

            val maj = versions(0).toByte
            val min = versions(1).toByte
            val pat = versions(2).toByte

            (maj, min, pat)
        } catch {
            case e: NumberFormatException =>
                throw new IllegalArgumentException(s"'$version' does not match this pattern : $PATTERN")
        }

        (major, minor, patch, stable)
    }

    private def checkPattern(success: Boolean, cause: Throwable = null)(implicit version: String): Unit = {
        if (!success)
            throw new IllegalArgumentException(s"'$version' does not match this pattern : $PATTERN", cause)
    }
}
