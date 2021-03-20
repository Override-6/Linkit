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

package fr.`override`.linkit.server.task

import fr.`override`.linkit.api.network.RemoteConsole
import fr.`override`.linkit.api.packet.traffic.PacketInjections.PacketInjection
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}
import fr.`override`.linkit.api.task.TaskTicket

import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class ConnectionTasksThread private(consoleErr: RemoteConsole,
                                    ticketQueue: BlockingQueue[TaskTicket],
                                    lostInjections: mutable.Map[Int, ListBuffer[PacketInjection]]) extends Thread with JustifiedCloseable {

    @volatile private var open = false
    @volatile private var currentTicket: TaskTicket = _

    def this(consoleErr: RemoteConsole, identifier: String) = {
        this(consoleErr, new ArrayBlockingQueue[TaskTicket](15000), mutable.Map.empty)
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
                    e.printStackTrace()
                    consoleErr.print(e)
            }
        }
    }

    override def close(reason: CloseReason): Unit = {
        if (currentTicket != null) {
            currentTicket.abort(reason)
            currentTicket = null
        }

        ticketQueue.clear()
        lostInjections.clear()
        open = false

        interrupt()
    }

    def copy(): ConnectionTasksThread =
        new ConnectionTasksThread(consoleErr, ticketQueue, lostInjections)

    private[task] def addTicket(ticket: TaskTicket): Unit = {
        ticketQueue.add(ticket)
    }

    private def executeNextTicket(): Unit = {
        val ticket = ticketQueue.take()
        currentTicket = ticket
        val channel = ticket.channel
        val taskID = channel.identifier
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
