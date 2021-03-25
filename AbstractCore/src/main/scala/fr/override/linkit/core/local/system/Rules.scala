package fr.`override`.linkit.core.local.system

object Rules {

    val MaxConnectionIDLength: Int = 16
    val ConnectionRefused: Byte = 1
    val ConnectionAccepted: Byte = 2

    val WPArgsLength: Byte = 3
    val WPArgsSeparator: Array[Byte] = ";".getBytes()


}
