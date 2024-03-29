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

package fr.linkit.api.gnom.network
import fr.linkit.api.gnom.network.tag.NameTag
import fr.linkit.api.gnom.referencing.{NetworkObjectReference, SystemObjectReference}

class EngineReference(val name: NameTag) extends NetworkReference with SystemObjectReference {

    override def asSuper: Option[NetworkObjectReference] = Some(NetworkReference)

    override def toString: String = {
        s"@network/${name.name}"
    }

    override def hashCode(): Int = name.hashCode

    override def equals(obj: Any): Boolean = obj match {
        case that: EngineReference => that.name == name
        case _ => false
    }

}
