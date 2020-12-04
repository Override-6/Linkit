package fr.overridescala.vps.ftp.api.system

import java.io.{PrintStream, PrintWriter, StringWriter}
import java.security.AccessController

import com.sun.corba.se.impl.orbutil.GetPropertyAction
import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}
import fr.overridescala.vps.ftp.api.packet.fundamental.DataPacket
import fr.overridescala.vps.ftp.api.utils.InactiveOutputStream


class RemoteConsole private(channel: PacketChannel.Async, output: PrintStream) extends PrintStream(InactiveOutputStream, true) {

    private val connected = channel.connectedID

    channel.onPacketReceived(e => {
        println(s"e = ${e}")
        e match {
            case data: DataPacket => output.println(s"[$connected]: ${new String(data.content)}")
            case other: Packet => throw new UnexpectedPacketException(s"Unexpected packet '${other.getClass.getName}' injected in a remote console.")
        }
    })

    override def write(b: Array[Byte]): Unit = {
        print(new String(b))
    }

    override def write(b: Int): Unit = {
        print(b)
    }

    override def write(buf: Array[Byte], off: Int, len: Int): Unit = {
        print(new String(buf.slice(off, off + len)))
    }

    override def print(obj: Any): Unit = {
        var str = if (obj == null) "null" else obj.toString

        if (obj.getClass.isArray)
            str = java.util.Arrays.deepToString(obj.asInstanceOf[Array[AnyRef]])

        channel.sendPacket(DataPacket(str))
    }

    override def print(x: Boolean): Unit = print(x: Any)

    override def print(c: Char): Unit = {
        print(c: Any)
    }

    override def print(s: Array[Char]): Unit = print(s: Any)

    override def print(str: String): Unit = if (str != lineSeparator) print(str: Any) //avoid newLine

    override def print(i: Int): Unit = print(i: Any)

    override def print(l: Long): Unit = print(l: Any)

    override def print(f: Float): Unit = print(f: Any)

    override def print(d: Double): Unit = print(d: Any)

    private val lineSeparator = AccessController.doPrivileged(new GetPropertyAction("line.separator"))

}

object RemoteConsole {

    class Err private[RemoteConsole](channel: PacketChannel.Async) extends RemoteConsole(channel, System.err) {
        def reportException(exception: Throwable): Unit = {

            val writer = new StringWriter()
            val stream = new PrintWriter(writer)
            exception.printStackTrace(stream)

            val stackTrace = stream.toString

            print(s"(This is a remote StackTrace from Relay '${channel.connectedID}')\n$stackTrace")
        }
    }

    def err(channel: PacketChannel.Async): Err = new Err(channel)

    def out(channel: PacketChannel.Async): RemoteConsole = new RemoteConsole(channel, System.out)

}
