package fr.overridescala.vps.ftp.api.packet

import java.io._
import java.net.InetSocketAddress

trait DynamicSocket extends Closeable {

    def remoteSocketAddress(): InetSocketAddress

    def read(buff: Array[Byte]): Int

    def write(buff: Array[Byte]): Unit

}
