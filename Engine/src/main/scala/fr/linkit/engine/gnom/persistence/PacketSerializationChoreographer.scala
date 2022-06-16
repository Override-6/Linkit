/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence

import fr.linkit.api.gnom.persistence.{PacketUpload, ObjectTranslator, TransferInfo}
import fr.linkit.engine.gnom.persistence.PacketSerializationChoreographer.MappedQueues

import scala.collection.mutable

class PacketSerializationChoreographer(translator: ObjectTranslator) {

    private val serialQueue = new MappedQueues[TransferInfo]()

    def add(transferInfo: TransferInfo)(onResultAvailable: PacketUpload => Unit): Unit = {
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
