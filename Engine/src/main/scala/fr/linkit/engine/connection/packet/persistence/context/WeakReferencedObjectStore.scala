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

package fr.linkit.engine.connection.packet.persistence.context

import fr.linkit.api.connection.packet.persistence.context.MutableReferencedObjectStore

import scala.collection.mutable

class WeakReferencedObjectStore() extends MutableReferencedObjectStore {

    private val codeToRef = mutable.WeakHashMap.empty[Int, AnyRef]
    private val refToCode = mutable.WeakHashMap.empty[AnyRef, Int]

    override def getReferenced(reference: Int): Option[AnyRef] = {
        codeToRef.get(reference)
    }

    override def getReferencedCode(reference: AnyRef): Option[Int] = {
        refToCode.get(reference)
    }

    override def ++=(refs: Map[Int, AnyRef]): this.type = {
        refs.foreachEntry((id, ref) => +=(id, ref))
        this
    }

    override def ++=(refs: AnyRef*): this.type = {
        refs.foreach(+=)
        this
    }

    def ++=(other: WeakReferencedObjectStore): this.type = {
        ++=(other.codeToRef.toMap: Map[Int, AnyRef])
        this
    }

    override def +=(anyRef: AnyRef): this.type = {
        if (anyRef eq null)
            -=(anyRef)
        else
            +=(anyRef.hashCode(), anyRef)
    }

    override def +=(code: Int, anyRef: AnyRef): this.type = {
        codeToRef.put(code, anyRef)
        refToCode.put(anyRef, code)
        this
    }

    override def -=(ref: AnyRef): this.type = {
        (refToCode remove ref).fold()(codeToRef.remove)
        this
    }
}
