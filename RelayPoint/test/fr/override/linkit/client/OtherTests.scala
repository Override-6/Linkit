package fr.`override`.linkit.client

import fr.`override`.linkit.api.packet.fundamental.ValPacket.IntPacket
import fr.`override`.linkit.api.packet.fundamental.WrappedPacket
import fr.`override`.linkit.api.packet.serialization.RawPacketSerializer
import fr.`override`.linkit.api.packet.{BroadcastPacketCoordinates, Packet, PacketCoordinates}
import fr.`override`.linkit.api.utils.Utils

import java.util.concurrent.ThreadLocalRandom
import scala.annotation.tailrec
import scala.util.control.NonFatal

object OtherTests {

    private val serializer = new RawPacketSerializer
    private val randomizer = ThreadLocalRandom.current()

    def main(args: Array[String]): Unit = try {
        makeSomething(1)
    } catch {
        case NonFatal(e) => e.printStackTrace()
    }


    @tailrec
    def makeSomething(times: Int): Unit = {

        val buffer = new Array[Byte](1000)
        randomizer.nextBytes(buffer)


        val packet = WrappedPacket("req",WrappedPacket("Global Shared Cache",IntPacket(-1)))
        val coords = BroadcastPacketCoordinates(444445, "bhap525252525245252852", true, "Bamboo", "koala")

        println("SERIALIZING...")
        val bytes = serialize(packet, coords)
        println()
        println()
        println()
        println(s"bytes = ${new String(bytes).replace('\r', ' ')} (length: ${bytes.length})")
        println()
        println()
        println()

        val sBytes = Utils.serialize(Array(packet, coords))
        val standardSerial = new String(sBytes).replace('\r', ' ')
        println("Standard method : " + standardSerial + s" (length: ${sBytes.length})")

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

    private def serialize(packet: Packet, coordinates: PacketCoordinates): Array[Byte] = {
        serializer.serialize(Array(coordinates, packet))
    }

    private def deserialize(bytes: Array[Byte]): (PacketCoordinates, Packet) = {
        val array = serializer.deserializeAll(bytes)
        (array(0).asInstanceOf[PacketCoordinates], array(1).asInstanceOf[Packet])
    }


    case class StreamPacket(buffer: Array[Byte]) extends Packet


}
