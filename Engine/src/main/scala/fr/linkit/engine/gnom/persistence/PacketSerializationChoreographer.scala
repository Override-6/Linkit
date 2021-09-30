package fr.linkit.engine.gnom.persistence

import fr.linkit.api.gnom.persistence.{ObjectSerializationResult, ObjectTranslator, TransferInfo}
import fr.linkit.engine.gnom.persistence.PacketSerializationChoreographer.MappedQueues

import scala.collection.mutable

class PacketSerializationChoreographer(translator: ObjectTranslator) {

    private val serialQueue = new MappedQueues[TransferInfo]()

    def add(transferInfo: TransferInfo)(onResultAvailable: ObjectSerializationResult => Unit): Unit = {
        val coords = transferInfo.coords
        serialQueue.enqueue(coords.path, transferInfo)(info => onResultAvailable(translator.translate(info)))
    }

}

object PacketSerializationChoreographer {

    class MappedQueues[A]() {
        val map = mutable.HashMap.empty[Array[Int], mutable.Queue[A]]

        def enqueue(path: Array[Int], a: A)(action: A => Unit): Unit = {
            var queue: mutable.Queue[A] = null
            map.synchronized {
                queue = map.getOrElseUpdate(path, mutable.Queue.empty[A])
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
