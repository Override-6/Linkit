package fr.`override`.linkit.api.utils

import scala.reflect.{ClassTag, classTag}
import scala.util.control.NonFatal

object ScalaUtils {

    def slowCopy[A: ClassTag](origin: Array[_ <: Any]): Array[A] = {
        val buff = new Array[A](origin.length)
        var i = 0
        try {
            origin.foreach(anyRef => {
                buff(i) = anyRef.asInstanceOf[A]
                i += 1
            })
            buff
        } catch {
            case NonFatal(e) =>
                println("Was Casting to " + classTag[A].runtimeClass)
                println(s"Origin = ${origin.mkString("Array(", ", ", ")")}")
                println(s"Failed when casting ref : ${origin(i)} at index $i")
                throw e
        }
    }

    def toInt(bytes: Array[Byte]): Int = {
        if (bytes == null || bytes.length != 4)
            throw new IllegalArgumentException("Invalid serialized int byte seq length")

        (0xff & bytes(0)) << 24 |
                ((0xff & bytes(1)) << 16) |
                ((0xff & bytes(2)) << 8) |
                ((0xff & bytes(3)) << 0)
    }

    def fromInt(value: Int): Array[Byte] = {
        Array[Byte](
            ((value >> 24) & 0xff).toByte,
            ((value >> 16) & 0xff).toByte,
            ((value >> 8) & 0xff).toByte,
            ((value >> 0) & 0xff).toByte
        )
    }

}
