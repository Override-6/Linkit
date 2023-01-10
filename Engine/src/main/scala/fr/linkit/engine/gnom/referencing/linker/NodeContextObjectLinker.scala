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

package fr.linkit.engine.gnom.referencing.linker

import fr.linkit.api.gnom.network.tag.EngineSelector
import fr.linkit.api.gnom.packet.PacketCoordinates
import fr.linkit.api.gnom.persistence.context.ContextualObjectReference
import fr.linkit.api.gnom.referencing.NetworkObject
import fr.linkit.api.gnom.referencing.linker.ContextObjectLinker
import fr.linkit.api.gnom.referencing.traffic.{LinkerRequestBundle, ObjectManagementChannel}
import fr.linkit.engine.gnom.referencing.presence.AbstractNetworkPresenceHandler
import fr.linkit.engine.gnom.referencing.{ContextObject, ObjectAlreadyReferencedException}
import fr.linkit.engine.internal.util.Identity
import org.jetbrains.annotations.Nullable

import java.util
import java.util.Map.Entry
import scala.util.Try

class NodeContextObjectLinker(@Nullable parent: ContextObjectLinker, omc: ObjectManagementChannel, selector: EngineSelector)
        extends AbstractNetworkPresenceHandler[ContextualObjectReference](Some(omc.traffic.getTrafficObjectLinker), omc, selector)
                with ContextObjectLinker {

    private val codeToRef = new util.HashMap[Int, AnyRef]()
    private val refToCode = new util.HashMap[AnyRef, Int]()

    override def findReferenceID(obj: AnyRef): Option[Int] = {
        var result = Try(refToCode.get(obj)).getOrElse(0)
        if (result == 0) result = refToCode.get(Identity(obj))
        if (result == 0) {
            if (parent != null) parent.findReferenceID(obj)
            else None
        } else Some(result)
    }

    override def findPersistableReference(obj: AnyRef, coords: PacketCoordinates): Option[Int] = {
        val result = findReferenceID(obj)
        if (result.isDefined && isPresentOnEngine(coords.senderTag, new ContextualObjectReference(coords.path, result.get))) {
            result
        } else {
            None
        }
    }

    override def findObject(reference: ContextualObjectReference): Option[NetworkObject[_ <: ContextualObjectReference]] = {
        val id     = reference.objectID
        val result = codeToRef.get(id)
        if (result == null && parent != null)
            return parent.findObject(reference)
        if (result eq null) None
        else {
            val obj = result match {
                case id: Identity[AnyRef] => id.obj
                case x                    => x
            }
            Option(new ContextObject(obj, reference))
        }
    }


    override def transferTo(linker: ContextObjectLinker): this.type = {
        linker ++= codeToRef
                .entrySet()
                .toArray(new Array[(Int, AnyRef)](_))
                .toMap
        this
    }

    override def injectRequest(bundle: LinkerRequestBundle): Unit = {
        bundle.linkerReference match {
            case _: ContextualObjectReference =>
                super.injectRequest(bundle)
            case _ if parent != null          =>
                parent.injectRequest(bundle)
        }
    }

    override def ++=(refs: Map[Int, AnyRef]): this.type = {
        refs.foreachEntry((id, ref) => +=(id, ref))
        this
    }

    override def putAllNotContained(refs: Map[Int, AnyRef]): this.type = {
        ++=(refs.filterNot(p => refToCode.containsKey(p._2)))
    }

    override def ++=(refs: AnyRef*): this.type = {
        refs.foreach(+=)
        this
    }

    def ++=(other: NodeContextObjectLinker): this.type = {
        ++=(other.codeToRef
                .entrySet()
                .toArray(new Array[Entry[Int, AnyRef]](_))
                .map(p => (p.getKey, p.getValue))
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
        if (code == 0)
            throw new IllegalArgumentException("Object code cannot be 0.")
        if (codeToRef.containsKey(code)) {
            throw new ObjectAlreadyReferencedException(s"Object $anyRef is already referenced with identifier '${codeToRef.get(code)}'.")
        }
        refToCode.put(anyRef, code)
        codeToRef.put(code, anyRef)
        this
    }

    override def -=(ref: AnyRef): this.type = {
        val code = refToCode.remove(ref)
        if (code == null) {
            codeToRef.remove(code)
        }
        this
    }

}
