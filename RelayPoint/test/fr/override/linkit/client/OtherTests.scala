package fr.`override`.linkit.client

import fr.`override`.linkit.api.network.cache.map.MapModification
import fr.`override`.linkit.api.packet.fundamental.{ValPacket, WrappedPacket}
import fr.`override`.linkit.api.packet.serialization.RawPacketSerializer
import fr.`override`.linkit.api.packet.{DedicatedPacketCoordinates, Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.Utils

import scala.annotation.tailrec
import scala.util.control.NonFatal

object OtherTests {

    private val serializer = new RawPacketSerializer

    def main(args: Array[String]): Unit = try {
        //makeSomething(1)

        assert(false)

        /*var h: Int = hash
        if (h == 0 && value.length > 0) {
            val `val`: Array[Char] = value
            for (i <- 0 until value.length) {
                h = 31 * h + `val`(i)
            }
            hash = h
        }
        h*/

    } catch {
        case NonFatal(e) => e.printStackTrace(Console.out)
    }


    @tailrec
    def makeSomething(times: Int): Unit = {
        import MapModification._
        val mod: (MapModification, Int, Any) = (PUT, 1, "cala.collection.immutable.Nil$")
        val packet = WrappedPacket("req", WrappedPacket("Global Shared Cache", ValPacket(mod)))
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
