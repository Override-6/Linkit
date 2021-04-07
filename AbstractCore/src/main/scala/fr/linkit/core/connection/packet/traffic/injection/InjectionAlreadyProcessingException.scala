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

package fr.linkit.core.connection.packet.traffic.injection

import fr.linkit.api.connection.packet.traffic.injection.PacketInjection
import fr.linkit.api.local.system.AppException

case class InjectionAlreadyProcessingException(injection: PacketInjection, msg: String) extends AppException(msg) {

    override protected def appendMessage(sb: StringBuilder): Unit = {
        super.appendMessage(sb)
        val coords = injection.coordinates
        sb.append(s"For injection into injectable id '${coords.injectableID}'\n")
                .append(s"Targeting ${coords.targetID}, sent by ${coords.senderID}\n")
    }

}
