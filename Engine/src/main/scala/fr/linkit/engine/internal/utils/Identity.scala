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

package fr.linkit.engine.internal.utils

class Identity[A](val obj: A) {

    override def hashCode(): Int = {
        System.identityHashCode(obj)
    }

    override def equals(obj: Any): Boolean = obj match {
        case id: Identity[A] => JavaUtils.sameInstance(id.obj, this.obj) //got an error "the result type of an implicit conversion must be more specific than AnyRef" if i put "obj eq this.obj"
        case _            => JavaUtils.sameInstance(obj, this.obj)
    }

    override def toString: String = obj.toString
}

object Identity {

    def apply[A](obj: A): Identity[A] = new Identity(obj)

}