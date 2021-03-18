package fr.`override`.linkit.core.local.utils

import java.io.OutputStream

object InactiveOutputStream extends OutputStream {
    override def write(b: Int): Unit = ()
}
