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

package fr.linkit.api.test

import org.junit.jupiter.api.function.{Executable, ThrowingSupplier}

import java.io.File
import java.util.Scanner

object TestUtils {

    val HomeProperty   : String = "LinkitHome"
    val DefaultHomePath: String = System.getenv("LOCALAPPDATA") + s"${File.separator}Linkit${File.separator}"

    implicit def toThrowingSupplier[T](action: => T): ThrowingSupplier[T] = () => action

    implicit def toExecutable[T](action: => T): Executable = () => action

    def getDefaultLinkitHome: String = {
        val homePath = System.getenv(HomeProperty)
        if (homePath != null)
            return homePath

        val scanner = new Scanner(System.in)
        //println(s"Environment variable '$HomeProperty' is not set !")
        //println(s"Would you like to set your own path for the Linkit Framework home ?")
        //println(s"Say 'y' to set your custom path, or something else to use default path '$DefaultHomePath'")
        print("> ")
        val response       = scanner.nextLine()
        val linkitHomePath = if (response.startsWith("y")) {
            //println("Enter your custom path (the folder will be created if it does not exists)")
            print("> ")
            scanner.nextLine()
        } else DefaultHomePath
        setEnvHome(linkitHomePath)
        //println(s"Linkit home path has been set to $linkitHomePath.")
        //println("(If this java process is a child from another process, such as an IDE")
        //println("You may restart the mother process in order to complete the environment variable update)")

        linkitHomePath
    }

    private def setEnvHome(linkitHomePath: String): Unit = {
        new File(linkitHomePath).createNewFile() //ensure that the folder exists.

        val osName     = System.getProperty("os.name").takeWhile(_ != ' ').trim.toLowerCase
        val setCommand = osName match {
            case "windows" | "ubuntu" => "setx"
            case "linux"              => "export"
        }

        val s = '\"'
        new ProcessBuilder("cmd", "/c", setCommand, HomeProperty, s"$s$linkitHomePath$s")
                .inheritIO()
                .start()
                .waitFor()
    }

}
