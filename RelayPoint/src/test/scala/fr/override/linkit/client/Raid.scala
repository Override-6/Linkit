package fr.`override`.linkit.client

import scala.io.StdIn

object Raid {


    def main(args: Array[String]): Unit = {
        print("Enter the number of connection you want to handle : ")
        val density = StdIn.readLine().toInt

        for (i <- 0 to density) {
            println()
            println()
            println(s"Connecting $i...")
            println()
            println()
            Main.launch(true, false, Main.LOCALHOST, i.toString)
        }

        println("RAID TERMINATED !!!")
    }

}
