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

package fr.linkit.api.internal.concurrency

trait Worker {

    val pool: WorkerPool

    val thread: Thread

    def taskRecursionDepth: Int

    def isSleeping: Boolean

    def getCurrentTask: Option[AsyncTask[_]]
    
    def getTaskStack: Array[Int]

    def getCurrentTaskID: Int = getCurrentTask.map(_.taskID).getOrElse(-1)

    def getController: WorkerThreadController

    @throws[IllegalThreadStateException]("if isSleeping = false")
    def runWhileSleeping(task: => Unit): Unit

}