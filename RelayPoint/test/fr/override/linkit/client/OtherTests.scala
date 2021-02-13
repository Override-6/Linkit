package fr.`override`.linkit.client

import fr.`override`.linkit.api.packet.fundamental.{ValPacket, WrappedPacket}
import fr.`override`.linkit.api.packet.serialization.RawPacketSerializer
import fr.`override`.linkit.api.packet.{DedicatedPacketCoordinates, Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.Utils

import scala.annotation.tailrec
import scala.util.control.NonFatal

object OtherTests {

    private val serializer = new RawPacketSerializer

    def main(args: Array[String]): Unit = try {
        // makeSomething(1)
        for (i <- 0 to 50000)
            println(i)
    } catch {
        case NonFatal(e) => e.printStackTrace(Console.out)
    }

    @tailrec
    def makeSomething(times: Int): Unit = {
        import fr.`override`.linkit.api.network.cache.map.MapModification._
        val packet = WrappedPacket("req", WrappedPacket("Global Shared Cache", WrappedPacket("14", ValPacket((PUT, 1624446167, "fr.override.linkit.api.packet.fundamental.ValPacket")))))
        val coords = DedicatedPacketCoordinates(11, "server", "a")

        println("SERIALIZING...")
        val bytes = serialize(packet, coords)
        println(s"bytes = ${new String(bytes)} (length: ${bytes.length})")
        val standardSerial = new String(Utils.serialize(packet))
        println("Standard method : " + standardSerial + s" (length: ${standardSerial.length})")

        println("DESERIALIZING...")
        println()
        println()
        val (coords0, packet0) = deserialize(bytes)
        println("packet, coords = " + (packet0, coords0))
        println(s"ref = ${(packet, coords)}")

        if (times > 0)
            makeSomething(times - 1)
    }

    private def serialize(packet: Packet, coordinates: PacketCoordinates): Array[Byte] = {
        serializer.serialize(Array(coordinates, packet))
    }

    private def deserialize(bytes: Array[Byte]): (PacketCoordinates, Packet) = {
        val array = serializer.deserializeAll(bytes)
        (array(0).asInstanceOf[PacketCoordinates], array(1).asInstanceOf[Packet])
    }


}
