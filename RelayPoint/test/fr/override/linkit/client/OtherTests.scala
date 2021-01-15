package fr.`override`.linkit.client

import scala.collection.mutable.ListBuffer

object OtherTests {


    def main(args: Array[String]): Unit = {
        val list = ListBuffer(1, 2, 3, 4, 5, 6, 7, 8, 9)
        val stream = list.to(LazyList).map(_ * 2)
        println(s"list = ${list}")
        println(s"stream = ${stream.mkString(", ")}")

        list += 10

        println(s"list = ${list}")
        println(s"stream = ${stream.mkString(", ")}")
    }


}
