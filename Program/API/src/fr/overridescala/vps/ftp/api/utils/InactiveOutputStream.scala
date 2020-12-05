package fr.overridescala.vps.ftp.api.utils

import java.io.OutputStream

object InactiveOutputStream extends OutputStream {
    override def write(b: Int): Unit = ()
}
