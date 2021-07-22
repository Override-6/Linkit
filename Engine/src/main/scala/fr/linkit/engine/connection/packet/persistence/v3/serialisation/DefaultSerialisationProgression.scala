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

import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.{DelegatingSerializerNode, SerializerNode}
import fr.linkit.api.connection.packet.persistence.v3.serialisation.{SerialisationOutputStream, SerialisationProgression}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.DefaultSerialisationProgression.Identity
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.HeadedInstanceNode
import fr.linkit.engine.local.utils.{JavaUtils, NumberSerializer}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DefaultSerialisationProgression extends SerialisationProgression {

    private val serializedInstances = mutable.HashMap.empty[Identity, DelegatingSerializerNode]
    /**
     * Contains all [[SerializerNode]] for object instances that will be reused in the body
     */
    private val pool                = ListBuffer.empty[SerializerNode]

    override def checkNode(obj: Any)(node: => SerializerNode): DelegatingSerializerNode = {
        if (containsInstance(obj)) {
            serializedInstances(obj) match {
                case DelegatingSerializerNode(h: HeadedInstanceNode) => DelegatingSerializerNode(h)
                case delegating                                      => delegating.delegated = {
                    pool += node
                    new HeadedInstanceNode(pool.length)
                }
            }
        }
        val wrappedNode = DelegatingSerializerNode(node)
        serializedInstances.put(obj, wrappedNode)
        wrappedNode
    }

    override def containsInstance(obj: Any): Boolean = serializedInstances.contains(obj)

    def writePool(out: SerialisationOutputStream): Unit = {
        out.write(NumberSerializer.serializeNumber(pool.size, true))
        pool.foreach(_.writeBytes(out))
    }

}

object DefaultSerialisationProgression {

    implicit class Identity(val obj: Any) {

        override def hashCode(): Int = System.identityHashCode(obj)

        override def equals(obj: Any): Boolean = obj match {
            case id: Identity => JavaUtils.sameInstance(id.obj, this.obj) //got an error "the result type of an implicit conversion must be more specific than AnyRef" if i put "obj eq this.obj"
            case _            => false
        }
    }
}
