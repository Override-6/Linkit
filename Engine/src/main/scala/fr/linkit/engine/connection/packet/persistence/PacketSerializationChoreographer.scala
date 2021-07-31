package fr.linkit.engine.connection.packet.persistence

import fr.linkit.api.connection.packet.persistence.{PacketSerializationResult, PacketTranslator, TransferInfo}
import fr.linkit.engine.connection.packet.persistence.PacketSerializationChoreographer.MappedQueues

import scala.collection.mutable

class PacketSerializationChoreographer(translator: PacketTranslator) {

    private val serialQueue = new MappedQueues[TransferInfo]()

    def add(transferInfo: TransferInfo)(onResultAvailable: PacketSerializationResult => Unit): Unit = {
        val coords = transferInfo.coords
        serialQueue.enqueue(coords.injectableID, transferInfo)(info => onResultAvailable(translator.translate(info)))
    }

}

object PacketSerializationChoreographer {

    class MappedQueues[A]() {
        val map = mutable.HashMap.empty[Int, mutable.Queue[A]]

        def enqueue(i: Int, a: A)(action: A => Unit): Unit = {
            var queue: mutable.Queue[A] = null
            map.synchronized {
                queue = map.getOrElseUpdate(i, mutable.Queue.empty[A])
                if (queue.nonEmpty)
                    return
                queue += a
            }
            var next = queue.removeHeadOption()
            while (next.isDefined) {
                val d = next.get
                action(d)
                next = queue.removeHeadOption()
            }
        }
    }
}
