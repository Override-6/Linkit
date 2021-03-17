package fr.`override`.linkit.client

import com.sun.glass.ui.Application
import fr.`override`.linkit.skull.connection.packet.fundamental.RefPacket.ArrayRefPacket
import fr.`override`.linkit.skull.connection.packet.fundamental.WrappedPacket
import fr.`override`.linkit.skull.connection.packet.{Packet, PacketCoordinates}

import java.util.concurrent.ThreadLocalRandom
import scala.annotation.tailrec
import scala.util.control.NonFatal

object OtherTests {

    private val serializer = LocalCachedObjectSerializer
    private val randomizer = ThreadLocalRandom.current()

    def main(args: Array[String]): Unit = try {
        /*Application.run(() => {
            makeSomething(1)
        })*/
        println(Console.RED + "HELLO")
    } catch {
        case NonFatal(e) => e.printStackTrace()
    }


    @tailrec
    def makeSomething(times: Int): Unit = {
        val packet = WrappedPacket("res", ArrayRefPacket(Array("server")))
        val coords = DedicatedPacketCoordinates(11, "a", "server")

        println(s"SERIALIZING... ($packet & $coords)")
        val t0 = System.currentTimeMillis()
        val bytes = serialize(packet, coords)
        val t1 = System.currentTimeMillis()
        println()
        println()
        println()
        println(s"bytes = ${new String(bytes).replace('\r', ' ')} (length: ${bytes.length}) took ${t1 - t0}ms")
        println()
        println()
        println()

        //val sBytes = Utils.serialize(Array(packet, coords))
        //val standardSerial = new String(sBytes).replace('\r', ' ')
        //println("Standard method : " + standardSerial + s" (length: ${sBytes.length})")
        //println("GSON method : " + new Gson().toJson(Array(coords, packet)) + s" (length: ${sBytes.length})")

        println("DESERIALIZING...")
        println()
        println()
        val (coords0, packet0) = deserialize(bytes)

        println(s"coords0 = ${coords0}")
        println(s"packet0 = ${packet0}")
        println(s"refCoords = ${coords}")
        println(s"refPacket = ${packet}")

        if (times > 1)
            makeSomething(times - 1)

    }

    protected def serializeInt(value: Int): Array[Byte] = {
        Array[Byte](
            ((value >> 24) & 0xff).toByte,
            ((value >> 16) & 0xff).toByte,
            ((value >> 8) & 0xff).toByte,
            ((value >> 0) & 0xff).toByte
        )
    }

    private def serialize(packet: Packet, coordinates: PacketCoordinates): Array[Byte] = {
        serializer.serialize(Array(coordinates, packet))
    }

    private def deserialize(bytes: Array[Byte]): (PacketCoordinates, Packet) = {
        val array = serializer.deserializeAll(bytes)
        (array(0).asInstanceOf[PacketCoordinates], array(1).asInstanceOf[Packet])
    }


    case class StreamPacket(buffer: Array[Int]) extends Packet

}
