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

import fr.linkit.api.connection.packet.PacketCoordinates
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.SerializerNode
import fr.linkit.api.connection.packet.persistence.v3.serialisation.{SerialisationOutputStream, SerialisationProgression, SerializationObjectPool}
import fr.linkit.api.connection.packet.persistence.v3.{PacketPersistenceContext, SerializableClassDescription}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.SerializerNodeFlags.{ClassFlag, MappedObjectFlag}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.node.{NullInstanceNode, SimpleObjectSerializerNode}
import fr.linkit.engine.local.utils.UnWrapper

import java.io.NotSerializableException
import java.lang.reflect.Modifier

class DefaultSerializationProgression(override val context: PacketPersistenceContext,
                                      override val pool: SerializationObjectPool,
                                      override val coordinates: PacketCoordinates,
                                      out: SerialisationOutputStream) extends SerialisationProgression {

    override def getSerializationNode(obj: Any): SerializerNode = {
        getPredefinedNode(obj).getOrElse {
            val clazz = obj.getClass
            //println(s"Getting Serialisation node for class '${clazz.getName}...' (class code = ${clazz.getName.hashCode}")
            if (clazz.isArray) //the above match does not works for primitive arrays
                return out.arrayNode(obj.asInstanceOf[Array[_]])
            if (clazz.isInterface || Modifier.isAbstract(clazz.getModifiers))
                throw new NotSerializableException(s"Could not serialize interface or abstract class ${clazz.getName}.")
            pool.checkNode(obj, out) {
                val objDesc       = context.getDescription[Any](clazz)
                val miniPersistor = objDesc.miniPersistor
                if (miniPersistor.isDefined) {
                    val mappedObject = miniPersistor.get.serialize(obj)
                    val node         = getPredefinedNode(mappedObject).getOrElse(serializeNode(mappedObject, context.getDescription[Any](mappedObject.getClass)))
                    out => {
                        val pos = out.position()
                        out.position(pos + 5)
                        node.writeBytes(out)
                        out.put(pos, MappedObjectFlag) //Replace the 'ObjectFlag' with 'MappedObjectFlag'
                        out.putInt(pos + 1, clazz.getName.hashCode)
                    }
                } else {
                    serializeNode(obj, objDesc)
                }
            }
        }
    }

    private def getPredefinedNode(obj: Any): Option[SerializerNode] = {
        val node: SerializerNode = obj match {
            case null | None                          => new NullInstanceNode(obj == None)
            case i if UnWrapper.isPrimitiveWrapper(i) => out.primitiveNode(i.asInstanceOf[AnyVal])
            case e: Enum[_]                           => SimpleObjectSerializerNode(out.enumNode(e))
            case str: String                          => out.stringNode(str)
            case array: Array[_]                      => out.arrayNode(array)
            case clazz: Class[_]                      => out =>
                out.put(ClassFlag)
                out.writeClass(clazz)
            case _                                    => null
        }
        Option(node)
    }

    private def serializeNode(obj: Any, desc: SerializableClassDescription[Any]): SerializerNode = {
        val persistor = desc.serialPersistor
        persistor.getSerialNode(obj, desc, context, this)
    }
}

