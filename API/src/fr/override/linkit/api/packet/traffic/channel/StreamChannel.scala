package fr.`override`.linkit.api.packet.traffic.channel

import java.io.{DataInputStream, DataOutputStream}

import fr.`override`.linkit.api.concurrency.RelayWorkerThreadPool
import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.Packet
import fr.`override`.linkit.api.packet.traffic.ChannelScope
import fr.`override`.linkit.api.packet.traffic.PacketInjections.PacketInjection
import org.jetbrains.annotations.Nullable

class StreamChannel(scope: ChannelScope) extends AbstractPacketChannel(scope) {
    @Nullable private var input: DataInputStream = _
    @Nullable private var output: DataOutputStream = _
    @volatile private var transferConstantly = false

    override def handleInjection(injection: PacketInjection): Unit = {
        val packets = injection.getPackets
        packets.foreach {
            case packet: StreamPacket =>
                output.write(packet.streamSlice)
            case p => throw new UnexpectedPacketException(s"Received forbidden packet $p")
        }
    }

    def transferAll(): Unit = {
        val available = input.available()
        val buff = new Array[Byte](available)
        input.readFully(buff)
        scope.sendToAll(new StreamPacket(buff))
    }

    def startConstantTransfer(): Unit = {
        RelayWorkerThreadPool.checkCurrentIsNotWorker("This worker thread can't be undefinitely locked.")
        transferConstantly = true
        while (transferConstantly) {
            transferAll()
        }
    }

    def stopConstantTransfer(): Unit = transferConstantly = false

    def inputStream: DataInputStream = input

    def outputStream: DataOutputStream = output

    def setInput(input: DataInputStream): Unit = this.input = input

    def setOutput(output: DataOutputStream): Unit = this.output = output

    private class StreamPacket(val streamSlice: Array[Byte]) extends Packet

}
