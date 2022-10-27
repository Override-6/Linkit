/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.debug

import fr.linkit.api.application.connection.ConnectionContext
import fr.linkit.api.internal.concurrency.Worker
import fr.linkit.engine.gnom.packet.traffic.AbstractPacketTraffic
import fr.linkit.engine.internal.debug.cli.SectionedPrinter

import java.io.PrintStream
import scala.collection.mutable
import scala.util.Try

object Debugger {

    private[debug] val threadStates = mutable.HashMap.empty[Thread, ThreadWorkStack]

    private val connections = mutable.Set.empty[ConnectionContext]

    private[linkit] def registerConnection(connectionContext: ConnectionContext): Unit = {
        connections += connectionContext
    }

    def push(step: => Step): Unit = {
        val stp = step
        currentStack.push(stp)
        stp match {
            case _: RequestStep => DeadlockWatchdog.notifyNewRequestPending()
            case _              =>
        }
    }

    def pop(): Step = {
        val stack = currentStack
        val step  = stack.pop()
        if (stack.isEmpty)
            threadStates.remove(stack.thread)
        step
    }

    private def currentStack: ThreadWorkStack = {
        val currentThread = Thread.currentThread()
        threadStates.getOrElseUpdate(currentThread, new ThreadWorkStack(currentThread))
    }

    def dumpWorkers(out: PrintStream = System.out): Unit = {
        val (workers, others) = threadStates.keys.partitionMap {
            case w: Worker => Left(w)
            case o         => Right(o)
        }
        val printer = new SectionedPrinter(out)
        workers.groupBy(_.pool).foreach { case (pool, poolWorkers) =>
            out.println(s"worker pool '${pool.name}': ")
            poolWorkers.toArray.sortBy(_.getName).foreach(t => Try(threadStates(t).printStack(printer)))
        }
        if (others.nonEmpty) {
            out.println("other threads:")
            others.toArray.sortBy(_.getName).foreach(t => Try(threadStates(t).printStack(printer)))
        }
    }

    def dumpTraffic(out: PrintStream = System.out): Unit = connections.foreach(dumpConnectionTraffic(_, out))

    def dumpConnectionTraffic(connection: ConnectionContext, out: PrintStream = System.out): Unit = {
        val connectionServerID = connection.network.serverName
        val printer = new SectionedPrinter(out)
        connection.traffic match {
            case traffic: AbstractPacketTraffic =>
                out.println(s"Dumping connection '$connectionServerID' traffic:")
                traffic.dump(printer)
            case _                              =>
                out.println(s"Could not dump connection '$connectionServerID' traffic.")
        }
    }

}
