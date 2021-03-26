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

package fr.linkit.api.connection.task

import fr.linkit.api.connection.packet.traffic.PacketChannel
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.linkit.api.local.system.Reason

import java.io.Closeable

/**
 * This class is the hearth of this program.
 * Tasks are registered and are enqueued, then executed one after the others
 *
 * <b><u>How tasks are created from packets :</u></b>
 *      1. First, all received packets are handled by the [[Relay]], then this TasksHandler.
 *         DataPackets have an identifier, this identifier is the identifier from which Task this packet is concerned.
 *         2. If the packet identifier differs from the current Task, it means the Relay received a new Task to schedule and enqueue
 *         DataPackets contains a identifier, a header and some content bytes
 *         3. The [[TaskCompleterHandler]] creates a TaskExecutor from the init packet header (which is the task name). Then registers the task.
 *
 *      <b>Notes:</b>
 *          if the packet identifier is equals to the current task identifier, the TasksHandler will add the packet to
 *          the used [[PacketChannel]]
 *
 * @see PacketChannel
 * */
trait TasksHandler extends Closeable {
    //TODO handle a task which want to complete with an unknown relay identifier
    //TODO use systemPacketChannels to

    /**
     * The relay identifier
     * */
    val identifier: String

    /**
     * @return the [[TaskCompleterHandler]]
     * @see [[TaskCompleterHandler]]
     * */
    val tasksCompleterHandler: TaskCompleterHandler

    /**
     * Handles the packet.
     * @param packet packet to handle
     *
     * @throws TaskException if the handling went wrong
     * */
    def handlePacket(packet: Packet, coordinates: DedicatedPacketCoordinates): Unit


    /**
     * Registers a task
     * @param executor the task to execute
     * @param taskIdentifier the task identifier
     * @param ownFreeWill true if the task was created by the user, false if the task comes from other Relay
     * */
    def schedule(executor: TaskExecutor, taskIdentifier: Int, targetID: String, ownFreeWill: Boolean): Unit

    /**
     * Suddenly stop a task execution and execute his successor.
     * */
    def skipCurrent(reason: Reason): Unit
}
