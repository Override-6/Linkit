/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.server.task

import fr.linkit.api.connection.packet.traffic.injection.PacketInjection
import fr.linkit.api.local.system.{AppLogger, JustifiedCloseable, Reason}
import fr.linkit.core.connection.task.TaskTicket

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ConnectionTasksThread private(ticketQueue: BlockingQueue[TaskTicket],
                                    lostInjections: mutable.Map[Int, ListBuffer[PacketInjection]]) extends Thread with JustifiedCloseable {

    @volatile private var open                      = false
    @volatile private var currentTicket: TaskTicket = _

    def this(identifier: String) = {
        this(new ArrayBlockingQueue[TaskTicket](15000), mutable.Map.empty)
        setName(s"RP Task Execution ($identifier)")
    }

    override def run(): Unit = {
        open = true
        while (open) {
            try {
                executeNextTicket()
            } catch {
                //normal exception thrown when the thread was suddenly stopped
                case _: InterruptedException =>
                case NonFatal(e) =>
                    AppLogger.printStackTrace(e)
                // consoleErr.print(e)
            }
        }
    }

    override def close(reason: Reason): Unit = {
        if (currentTicket != null) {
            currentTicket.abort()
            currentTicket = null
        }

        ticketQueue.clear()
        lostInjections.clear()
        open = false

        interrupt()
    }

    def copy(): ConnectionTasksThread =
        new ConnectionTasksThread(ticketQueue, lostInjections)

    private[task] def addTicket(ticket: TaskTicket): Unit = {
        ticketQueue.add(ticket)
    }

    private def executeNextTicket(): Unit = {
        val ticket = ticketQueue.take()
        currentTicket = ticket
        val channel = ticket.channel
        val taskID  = channel.identifier
        //Adding eventual lost packets to this task
        if (lostInjections.contains(taskID)) {
            val queue = lostInjections(taskID)
            queue.foreach(channel.inject)
            queue.clear()
            lostInjections.remove(taskID)
        }
        ticket.start()
    }

    override def isClosed: Boolean = !open
}
