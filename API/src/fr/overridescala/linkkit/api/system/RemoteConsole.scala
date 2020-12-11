package fr.overridescala.linkkit.api.system

import java.io.{PrintStream, PrintWriter, StringWriter}
import java.security.AccessController

import fr.overridescala.linkkit.api.exceptions.UnexpectedPacketException
import fr.overridescala.linkkit.api.packet.fundamental.DataPacket
import fr.overridescala.linkkit.api.packet.Packet
import fr.overridescala.linkkit.api.packet.channel.PacketChannel
import fr.overridescala.linkkit.api.utils.InactiveOutputStream
import org.jetbrains.annotations.Nullable
import sun.security.action.GetPropertyAction


class RemoteConsole private(@Nullable channel: PacketChannel.Async,
                            @Nullable output: PrintStream) extends PrintStream(InactiveOutputStream, true) {

    private val connected = channel.connectedID

    if (channel != null || output != null)
        channel.onPacketReceived {
            case data: DataPacket => output.println(s"[$connected]: ${new String(data.content)}")
            case other: Packet => throw new UnexpectedPacketException(s"Unexpected packet '${other.getClass.getName}' injected in a remote console.")
        }

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

        channel.sendPacket(DataPacket("", str))
    }

    override def print(x: Boolean): Unit = print(x: Any)

    override def print(c: Char): Unit = {
        print(c: Any)
    }

    override def print(s: Array[Char]): Unit = print(s: Any)
                                                        //avoid newLine
    override def print(str: String): Unit = if (str != lineSeparator) print(str: Any)

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

            val stackTrace = writer.toString

            var message = s"(This is a remote StackTrace (RST) from Relay '${channel.ownerID}')\n$stackTrace"
            message = message.slice(0, message.length - 1)

            this.print(message.replace("\n", "\nRST > "))
        }


        def reportExceptionSimplified(exception: Throwable): Unit = {
            val sb = new StringBuilder
            var cause = exception
            while (cause != null) {
                val name = cause.getClass.getSimpleName
                val message = cause.getMessage
                if (cause != exception)
                    sb.append("     caused by ")
                sb.append(name)
                if (message != null) {
                    sb.append(": ")
                            .append(message)
                }

                cause = cause.getCause
            }
            val causes = sb.toString()
            print(s"(an exception occurred in relay '${channel.ownerID}') simplified description : \n$causes")
        }
    }

    def err(channel: PacketChannel.Async): Err = new Err(channel)

    def out(channel: PacketChannel.Async): RemoteConsole = new RemoteConsole(channel, System.out)

    def mock(): RemoteConsole = new RemoteConsole(null, null)

    def mockErr(): RemoteConsole.Err = new RemoteConsole.Err(null)

}
