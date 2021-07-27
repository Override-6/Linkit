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
import fr.linkit.api.connection.packet.persistence.v3.serialisation.{SerialisationObjectPool, SerialisationOutputStream}
import fr.linkit.engine.connection.packet.persistence.v3.ArraySign
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.DefaultSerialisationProgression.Identity
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.HeadedInstanceNode
import fr.linkit.engine.local.utils.NumberSerializer

import java.nio.ByteBuffer
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DefaultSerialisationObjectPool() extends SerialisationObjectPool {

    private val serializedInstances = mutable.HashMap.empty[Identity, DelegatingSerializerNode]
    /**
     * Contains all [[SerializerNode]] for object instances that will be reused in the body
     */
    private val pool                = ListBuffer.empty[Any]
    private var isWritingPool       = false
    private var depth: Int          = 0

    override def checkNode(obj: Any, out: SerialisationOutputStream)(nodeSupplier: => SerializerNode): DelegatingSerializerNode = {
        if (containsInstance(obj)) {
            if (isWritingPool && depth <= 1) {
                val node = serializedInstances(obj).deserializer
                if (node == null)
                    throw new NullPointerException("Unexpected null node deserializer")
                return DelegatingSerializerNode(node)
            }
            return changeDelegate(obj)
        }
        val a = DelegatingSerializerNode(null)
        serializedInstances.put(obj, a)
        val node = nodeSupplier
        val b    = DelegatingSerializerNode(node)
        if (a.delegated == null) {
            serializedInstances.put(obj, b)
            b.deserializer = node
            b
        } else {
            a.deserializer = node
            a
        }
    }

    override def containsInstance(obj: Any): Boolean = serializedInstances.contains(Identity(obj))

    override def addSerialisationDepth(): Unit = depth += 1

    override def removeSerialisationDepth(): Unit = {
        if (depth > 0)
            depth -= 1
    }

    def writePool(out: SerialisationOutputStream): Unit = {
        isWritingPool = true
        val fakeOut = new DefaultSerialisationOutputStream(ByteBuffer.allocate(out.capacity()), this, out.progression.context)
        val array   = ArraySign.out(pool.toSeq, fakeOut.progression).getNode
        array.writeBytes(fakeOut)
        out.write(NumberSerializer.serializeNumber(fakeOut.position(), true))
        out.write(NumberSerializer.serializeNumber(pool.size, true))
        out.put(fakeOut.buff.flip())
        isWritingPool = false
    }

    private def changeDelegate(obj: Any): DelegatingSerializerNode = {
        serializedInstances(Identity(obj)) match {
            case DelegatingSerializerNode(h: HeadedInstanceNode) => DelegatingSerializerNode(h)
            case delegating                                      =>
                pool += obj
                delegating.delegated = {
                    new HeadedInstanceNode(pool.size - 1)
                }
                /*if (obj.isInstanceOf[String] || UnWrapper.isPrimitiveWrapper(obj))
                    return delegating
                context.getDescription(obj.getClass).serializableFields.foreach(field => {
                    val fieldValue = field.first.get(obj)
                    val node       = context.getSerializationNode(fieldValue, out, this)
                    changeDelegate(fieldValue, out, node)
                })*/
                delegating
        }
    }
}
