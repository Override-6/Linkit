package fr.linkit.engine.internal.utils

import org.jetbrains.annotations.NotNull

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ArrayByteInserter(@NotNull val origin: Array[Byte]) {

    protected val buff            : ListBuffer[Byte]       = ListBuffer.from(origin)
    private   val insertionHistory: ListBuffer[(Int, Int)] = mutable.ListBuffer.empty[(Int, Int)] //(Index, Shift)
    private   val ignoredBPZones  : ListBuffer[(Int, Int)] = ListBuffer.empty[(Int, Int)]

    def isIgnoredPosition(pos: Int): Boolean = {
        ignoredBPZones.exists(zone => pos >= zone._1 && pos <= zone._2)
    }

    def insertBytes(pos: Int, bytes: Array[Byte]): Unit = {
        val shift = getShiftAt(pos)
        buff.insertAll(pos + shift, bytes)
        archiveShift(pos, bytes.length)
    }

    def deleteBlock(pos: Int, length: Int): Unit = {
        val shift = getShiftAt(pos)
        val idx   = pos + shift
        val len   = length - (shift - getShiftAt(pos + length))
        buff.remove(idx, len)
        archiveShift(pos + len, -len)
        ignoredBPZones += ((pos, pos + len))
    }

    def getShiftAt(pos: Int): Int = {
        insertionHistory.takeWhile(_._1 < pos).map(_._2).sum
    }

    protected def archiveShift(pos: Int, shift: Int): Unit = {
        def insert(shift: Int): Unit = {
            val idx = insertionHistory.lastIndexWhere(_._1 <= pos)
            if (insertionHistory.exists(_._1 == pos))
                insertionHistory.update(idx, (pos, shift))
            else insertionHistory.insert(idx + 1, (pos, shift))
        }

        insertionHistory.find(_._1 == pos).fold(insert(shift)) { (pair) =>
            insert(shift + pair._2)
        }
    }

    def getResult: Array[Byte] = buff.toArray

}