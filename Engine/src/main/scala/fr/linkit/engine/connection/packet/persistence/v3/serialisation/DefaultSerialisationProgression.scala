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
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.{SerialisationObjectPool, SerialisationOutputStream, SerialisationProgression}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.NullInstanceNode
import fr.linkit.engine.local.utils.{JavaUtils, UnWrapper}

import java.io.NotSerializableException
import java.lang.reflect.Modifier

class DefaultSerialisationProgression(override val context: PersistenceContext,
                                      override val pool: SerialisationObjectPool,
                                      out: SerialisationOutputStream) extends SerialisationProgression {

    override def getSerializationNode(obj: Any): SerializerNode = {
        obj match {
            case null | None                          => new NullInstanceNode(obj == None)
            case i if UnWrapper.isPrimitiveWrapper(i) => out.writePrimitive(i.asInstanceOf[AnyVal])
            case e: Enum[_]                           => out.writeEnum(e)
            case str: String                          => out.writeString(str)
            case array: Array[Any]                    => out.writeArray(array)
            case _                                    =>
                val clazz = obj.getClass
                if (clazz.isInterface || Modifier.isAbstract(clazz.getModifiers))
                    throw new NotSerializableException(s"Could not serialize interface or abstract class ${clazz.getName}.")
                val desc = context.getDescription(clazz)
                pool.checkNode(obj, out) {
                    context.getPersistence(clazz).getSerialNode(obj, desc, context, this)
                }
        }
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
