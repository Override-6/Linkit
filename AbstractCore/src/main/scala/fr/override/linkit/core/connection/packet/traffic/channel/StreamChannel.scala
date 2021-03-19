package fr.`override`.linkit.core.connection.packet.traffic.channel

import fr.`override`.linkit.internal.concurrency.BusyWorkerPool
import fr.`override`.linkit.api.connection.packet.Packet
import fr.`override`.linkit.api.connection.packet.traffic.ChannelScope
import .PacketInjection
import org.jetbrains.annotations.Nullable

import java.io.{DataInputStream, DataOutputStream}

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
        BusyWorkerPool.checkCurrentIsNotWorker("This worker thread can't be undefinitely locked.")
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
