package fr.`override`.linkit.api.utils

import scala.reflect.ClassTag

object ScalaUtils {

    def cloneArray[B : ClassTag](array: Array[AnyRef]): Array[B] = {
        val destination = new Array[B](array.length)
        copyToArray(array, destination, 0, array.length)
        destination
    }

    def copyToArray[A, B](origin: Array[A], dest: Array[B], start: Int, len: Int): Int = {
        val copied = elemsToCopyToArray(origin.length, dest.length, start, len)
        if (copied > 0) {
            Array.copy(origin, 0, dest, start, copied)
        }
        copied
    }

    def elemsToCopyToArray(srcLen: Int, destLen: Int, start: Int, len: Int): Int =
        math.max(math.min(math.min(len, srcLen), destLen - start), 0)

}
