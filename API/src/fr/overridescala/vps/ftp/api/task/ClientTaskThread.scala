package fr.overridescala.vps.ftp.api.task

import java.util
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import fr.overridescala.vps.ftp.api.packet.{DataPacket, SimplePacketChannel}

class ClientTaskThread(private val packetChannel: SimplePacketChannel) extends Thread {

    private val queue: BlockingQueue[DataPacket] = new ArrayBlockingQueue[DataPacket](-1)

    override def run(): Unit = {

    }

    def addExecutor(taskExecutor: TaskExecutor): Unit = {
        queue
    }


}
