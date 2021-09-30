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

package fr.linkit.engine.gnom.packet.traffic.injection

import fr.linkit.api.gnom.packet.traffic.injection.PacketInjection
import fr.linkit.api.internal.system.AppException

case class InjectionAlreadyProcessingException(injection: PacketInjection, msg: String) extends AppException(msg) {

    override protected def appendMessage(sb: StringBuilder): Unit = {
        super.appendMessage(sb)
        val path = injection.injectablePath
        sb.append(s"For injection into packet injectable at '${path.mkString("/")}'\n")
    }

}
