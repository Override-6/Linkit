package fr.`override`.linkit.client

import fr.`override`.linkit.api.packet.fundamental.{PairPacket, WrappedPacket}
import fr.`override`.linkit.api.packet.serialization.RawPacketSerializer
import fr.`override`.linkit.api.system.Version
import fr.`override`.linkit.api.utils.Utils

object OtherTests {

    def main(args: Array[String]): Unit = {
        val ref = new WrappedPacket("Heyyee", new WrappedPacket("Salutations", new PairPacket("15", Version("ABC", "25.12.24", true))))
        val serializer = new RawPacketSerializer

        println("SERIALIZING...")
        val bytes = serializer.serialize(ref)
        println(s"bytes = ${new String(bytes)}")
        println("Standard Method : " + new String(Utils.serialize(ref)))

        println("DESERIALIZING...")
        val packet = serializer.deserialize(bytes)
        println("packet = " + packet + "; ref = " + ref)
    }


    def makeSomething(i: Int): Unit = {
        /*no-op*/
    }


}
