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

package fr.linkit.engine.gnom.referencing.linker

import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.persistence.context.ContextualObjectReference
import fr.linkit.api.gnom.referencing.linker.ContextObjectLinker
import fr.linkit.api.gnom.referencing.presence.{NetworkObjectPresence, ObjectPresenceState}
import fr.linkit.api.gnom.referencing.traffic.LinkerRequestBundle
import fr.linkit.api.gnom.referencing.{NetworkObject, NetworkObjectReference}
import fr.linkit.engine.gnom.persistence.config.PersistenceConfigBuilder
import fr.linkit.engine.gnom.referencing.presence.SystemNetworkObjectPresence
import fr.linkit.engine.gnom.referencing.{ContextObject, ObjectAlreadyReferencedException}
import fr.linkit.engine.internal.utils.Identity

import scala.collection.mutable

/**
 * Special [[ContextObjectLinker]] for the
 * [[fr.linkit.api.gnom.referencing.traffic.ObjectManagementChannel]]
 * */
class ObjectChannelContextObjectLinker(builder: PersistenceConfigBuilder) extends ContextObjectLinker {

    private val codeToRef = mutable.HashMap.empty[Int, AnyRef]
    private val refToCode = mutable.HashMap.empty[AnyRef, Int]
    builder.forEachRefs((id, ref) => +=(id, ref))

    override def isAssignable(reference: NetworkObjectReference): Boolean = reference.isInstanceOf[ContextualObjectReference]

    override def findReferenceID(obj: AnyRef): Option[Int] = refToCode.get(obj)

    override def findPersistableReference(obj: AnyRef, coords: PacketCoordinates): Option[Int] = findReferenceID(obj)

    override def ++=(refs: Map[Int, AnyRef]): this.type = {
        refs.foreachEntry((id, ref) => +=(id, ref))
        this
    }

    override def putAllNotContained(refs: Map[Int, AnyRef]): this.type = {
        ++=(refs.filterNot(p => refToCode.contains(p._2)))
    }

    override def ++=(refs: AnyRef*): this.type = {
        refs.foreach(+=)
        this
    }

    override def +=(anyRef: AnyRef): this.type = {
        if (anyRef eq null)
            -=(anyRef)
        else
            +=(anyRef.hashCode(), anyRef)
    }

    override def +=(code: Int, anyRef: AnyRef): this.type = {
        if (code == 0)
            throw new IllegalArgumentException("Object code cannot be 0.")
        if (codeToRef.contains(code)) {
            throw new ObjectAlreadyReferencedException(s"Object $anyRef is already referenced with identifier '${codeToRef.get(code)}'.")
        }
        refToCode.put(anyRef, code)
        codeToRef.put(code, anyRef)
        this
    }

    override def -=(ref: AnyRef): this.type = {
        val code = refToCode.remove(ref)
        if (code.isEmpty) {
            codeToRef.remove(code.get)
        }
        this
    }

    override def transferTo(linker: ContextObjectLinker): ObjectChannelContextObjectLinker.this.type = {
        linker ++= codeToRef.toMap
        this
    }

    override def injectRequest(bundle: LinkerRequestBundle): Unit = {
        throw new UnsupportedOperationException()
    }

    override def findObject(reference: ContextualObjectReference): Option[NetworkObject[_ <: ContextualObjectReference]] = {
        codeToRef.get(reference.objectID).map(new ContextObject(_, reference))
    }

    override def getPresence(ref: ContextualObjectReference): Option[NetworkObjectPresence] = Some(SystemNetworkObjectPresence)

    override def isPresentOnEngine(engineId: String, ref: ContextualObjectReference): Boolean = {
        true //ObjectManagementChannel only uses objects that are mandatory on all engines
    }
}
