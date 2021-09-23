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

import java.lang.ref.{Reference, ReferenceQueue, WeakReference}
import java.util
import java.util.Map.Entry

class WeakReferencedObjectStore() extends MutableReferencedObjectStore {

    private val codeToRef = new util.HashMap[Int, WeakReference[AnyRef]]()
    private val refToCode = new util.WeakHashMap[AnyRef, Int]()

    override def getReferenced(reference: Int): Option[AnyRef] = {
        val referenced = codeToRef.get(reference)
        if (referenced eq null) None
        else Some(referenced.get())
    }

    override def getReferencedCode(reference: AnyRef): Option[Int] = {
        Option(refToCode.get(reference))
    }

    override def ++=(refs: Map[Int, AnyRef]): this.type = {
        refs.foreachEntry((id, ref) => +=(id, ref))
        this
    }

    override def putAllNotContained(refs: Map[Int, AnyRef]): WeakReferencedObjectStore.this.type = {
        ++=(refs.filterNot(p => refToCode.containsKey(p._2)))
    }

    override def ++=(refs: AnyRef*): this.type = {
        refs.foreach(+=)
        this
    }

    def ++=(other: WeakReferencedObjectStore): this.type = {
        ++=(other.codeToRef
                .entrySet()
                .toArray(new Array[Entry[Int, WeakReference[AnyRef]]](_))
                .map(p => (p.getKey, p.getValue.get()))
                .toMap)
        this
    }

    override def +=(anyRef: AnyRef): this.type = {
        if (anyRef eq null)
            -=(anyRef)
        else
            +=(anyRef.hashCode(), anyRef)
    }

    override def +=(code: Int, anyRef: AnyRef): this.type = {
        if (refToCode.put(anyRef, code) != null) {
            throw new ObjectAlreadyReferencedException(s"Object $anyRef is already referenced with identifier '${codeToRef.get(code)}'.")
        }
        codeToRef.put(code, newWeakReference(anyRef))
        this
    }

    private def newWeakReference(ref: AnyRef): WeakReference[AnyRef] = {
        new WeakReference[AnyRef](ref, eventQueue)
    }

    override def -=(ref: AnyRef): this.type = {
        val code = refToCode.remove(ref)
        if (code != null) {
            codeToRef.remove(code)
        }
        this
    }

    private val eventQueue = new ReferenceQueue[AnyRef]() {
        override def poll(): Reference[_ <: AnyRef] = {
            val code = refToCode.get(super.poll())
            codeToRef.remove(code)
        }
    }
}
