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

package fr.linkit.engine.connection.packet.persistence.serializor.write

import fr.linkit.api.connection.packet.persistence.context.TypeProfile
import fr.linkit.api.connection.packet.persistence.obj.InstanceObject

class PacketObject(override val value: AnyRef,
                   val decomposed: Array[Any],
                   override val profile: TypeProfile[_]) extends InstanceObject[AnyRef] {

    override def equals(obj: Any): Boolean = obj == this.value

    override def hashCode(): Int = value.hashCode()
}
