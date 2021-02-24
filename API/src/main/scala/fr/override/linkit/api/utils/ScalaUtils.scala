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

}
