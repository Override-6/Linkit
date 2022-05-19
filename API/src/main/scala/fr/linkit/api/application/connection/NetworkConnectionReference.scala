/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.api.application.connection

import fr.linkit.api.gnom.network.NetworkReference
import fr.linkit.api.gnom.reference.NetworkObjectReference

class NetworkConnectionReference extends NetworkReference {

    override def asSuper: Option[NetworkObjectReference] = Some(NetworkReference)

    override def toString: String = "@network/connection"

    override def hashCode(): Int = 32

    override def equals(obj: Any): Boolean = obj.isInstanceOf[NetworkConnectionReference]
}

object NetworkConnectionReference extends NetworkConnectionReference