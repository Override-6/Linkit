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

package fr.linkit.engine.connection.packet.persistence.v3.serialisation

import fr.linkit.api.connection.packet.persistence.v3.PersistenceContext
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.{DelegatingSerializerNode, SerializerNode}
import fr.linkit.api.connection.packet.persistence.v3.serialisation.{SerialisationOutputStream, SerialisationProgression}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.DefaultSerialisationProgression.Identity
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.HeadedInstanceNode
import fr.linkit.engine.local.utils.{JavaUtils, NumberSerializer, UnWrapper}

import java.nio.ByteBuffer
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DefaultSerialisationProgression(context: PersistenceContext) extends SerialisationProgression {

    private val serializedInstances = mutable.HashMap.empty[Identity, DelegatingSerializerNode]
    /**
     * Contains all [[SerializerNode]] for object instances that will be reused in the body
     */
    private val pool                = ListBuffer.empty[(SerializerNode, Any)]

    private var isWritingPool = false
    private var depth         = 0

    override def checkNode(obj: Any, out: SerialisationOutputStream)(node: => SerializerNode): DelegatingSerializerNode = {
        if (containsInstance(obj)) {
            if (isWritingPool && depth <= 1) {
                val node = pool.find(p => JavaUtils.sameInstance(p._2, obj))
                return DelegatingSerializerNode(node.get._1)
            }
            return changeDelegate(obj, out, node)
        }
        val wrappedNode = DelegatingSerializerNode(node)
        if (!isWritingPool)
            serializedInstances.put(obj, wrappedNode)
        wrappedNode
    }

    override def containsInstance(obj: Any): Boolean = serializedInstances.contains(Identity(obj))

    override def addSerialisationDepth(): Unit = depth += 1

    override def removeSerialisationDepth(): Unit = {
        if (depth > 0)
            depth -= 1
    }

    private def changeDelegate(obj: Any, out: SerialisationOutputStream, node: => SerializerNode): DelegatingSerializerNode = {
        serializedInstances(obj) match {
            case DelegatingSerializerNode(h: HeadedInstanceNode) => DelegatingSerializerNode(h)
            case delegating                                      =>
                pool += ((node, obj))
                delegating.delegated = {
                    new HeadedInstanceNode(pool.size - 1)
                }
                if (obj.isInstanceOf[String] || UnWrapper.isPrimitiveWrapper(obj))
                    return delegating
                context.getDescription(obj.getClass).serializableFields.foreach(field => {
                    val fieldValue = field.first.get(obj)
                    val node       = context.getSerializationNode(fieldValue, out, this)
                    changeDelegate(fieldValue, out, node)
                })
                delegating
        }
    }

    def writePool(out: SerialisationOutputStream, context: PersistenceContext): Unit = {
        isWritingPool = true
        val fakeOut = new DefaultSerialisationOutputStream(ByteBuffer.allocate(out.capacity()), this, context)
        out.writeArray(pool.map(_._2).toArray).writeBytes(fakeOut)
        out.write(NumberSerializer.serializeNumber(fakeOut.position(), true))
        out.put(fakeOut.buff.flip())
        isWritingPool = false
    }

}

object DefaultSerialisationProgression {

    implicit class Identity(val obj: Any) {

        override def hashCode(): Int = System.identityHashCode(obj)

        override def equals(obj: Any): Boolean = obj match {
            case id: Identity => JavaUtils.sameInstance(id.obj, this.obj) //got an error "the result type of an implicit conversion must be more specific than AnyRef" if i put "obj eq this.obj"
            case _            => false
        }

        override def toString: String = obj.toString
    }
}
