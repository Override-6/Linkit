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

import fr.linkit.api.connection.packet.channel.PacketChannel
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.linkit.api.local.system.Reason

import java.io.Closeable

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
