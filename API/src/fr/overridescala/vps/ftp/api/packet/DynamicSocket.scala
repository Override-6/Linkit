package fr.overridescala.vps.ftp.api.packet

import java.io._
import java.net.SocketAddress

trait DynamicSocket extends Closeable {

    def remoteSocketAddress(): SocketAddress

    def read(buff: Array[Byte]): Int

    def write(buff: Array[Byte]): Unit

}
