/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.api.connection.packet.traffic.injection

import fr.linkit.api.connection.packet.traffic.PacketInjectable
import fr.linkit.api.local.concurrency.workerExecution

trait PacketInjectionController extends PacketInjection {

    @workerExecution
    def processOrElse(processAction: => Unit)(orElse: => Unit): Unit

    /**
     * This method takes effect only once, and thus perform injection
     * only if it didn't done it before.
     *
     * @return true if given injectables received their injection.
     * */
    def performPinAttach(injectables: Iterable[PacketInjectable]): Boolean

    /**
     * Notifies that all attached pins may be processed. <br>
     * Each callbacks of pins will be called if the implementation
     * decides to do it for the current thread.
     * */
    @workerExecution
    def processRemainingPins(): Unit

    def isProcessing: Boolean

}
