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

package fr.linkit.api.gnom.referencing


class NamedIdentifier(private val name: Option[String], private val id: Int) {
    override def equals(id: Any): Boolean = id match {
        case ni: NamedIdentifier => ni.name == name && ni.id == this.id
        case _                   => false
    }

    override def toString: String = {
        if (name.nonEmpty)
            name.get + "@" + Integer.toHexString(id)
        else id.toString
    }

    override def hashCode(): Int = Seq(name, id).hashCode()
}

object NamedIdentifier {
    implicit def apply(id: Int): NamedIdentifier = new NamedIdentifier(None, id)
    def apply(name: String, id: Int): NamedIdentifier = new NamedIdentifier(Some(name), id)
}

