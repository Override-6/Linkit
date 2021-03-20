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

import fr.`override`.linkit.server.connection.ConnectionSession

class ConnectionTasksHandler(session: ConnectionSession) extends TasksHandler {

  private val server = session.server
  private val systemChannel = session.channel
  override val tasksCompleterHandler: TaskCompleterHandler = server.taskCompleterHandler
  override val identifier: String = session.identifier
  private var tasksThread = new ConnectionTasksThread(session.errConsole, identifier)
  tasksThread.start()


  /**
   * Handles the packet.
   *
   * @param packet packet to handle
   * @throws TaskException if the handling went wrong
   * */
  override def handlePacket(packet: TaskInitPacket, coordinates: DedicatedPacketCoordinates): Unit = {
    try {
      tasksCompleterHandler.handleCompleter(packet, coordinates, this)
    } catch {
      case e: TaskException =>
        Console.err.println(e.getMessage)
        systemChannel.sendOrder(SystemOrder.ABORT_TASK, CloseReason.INTERNAL_ERROR)
        session.errConsole.print(e)
    }
  }

  /**
   * Registers a task
   *
   * @param executor       the task to execute
   * @param taskIdentifier the task identifier
   * @param ownFreeWill    true if the task was created by the user, false if the task comes from other Relay
   * */
  override def schedule(executor: TaskExecutor, taskIdentifier: Int, targetID: String, ownFreeWill: Boolean): Unit = {
    if (targetID == server.identifier)
      throw new TaskException("can't schedule any task execution from server to server !")

    val ticket = new TaskTicket(executor, server, taskIdentifier, targetID, ownFreeWill)
    tasksThread.addTicket(ticket)
  }

  /**
   * closes the current client tasks thread
   * */
  override def close(reason: CloseReason): Unit = {
    tasksThread.close(reason)
  }

  /**
   * Suddenly stop a task execution and execute his successor.
   * */
  override def skipCurrent(reason: CloseReason): Unit = {
    //Restarting the thread causes the current task to be skipped
    //And wait or execute the task that come after it
    val lastThread = tasksThread
    tasksThread = tasksThread.copy()
    lastThread.close(reason)
    tasksThread.start()
  }

  override def isClosed: Boolean = tasksThread.isClosed
}