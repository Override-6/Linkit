package fr.`override`.linkit.core.local.system

import java.util.regex.Pattern

object Rules {

    val MaxConnectionIDLength: Int = 16
    val ConnectionRefused: Byte = 1
    val ConnectionAccepted: Byte = 2

    val WPArgsLength: Byte = 3
    val WPArgsSeparator: Array[Byte] = ";".getBytes()

    val IdentifierPattern: Pattern = Pattern.compile("^\\w{0,16}$")

}
