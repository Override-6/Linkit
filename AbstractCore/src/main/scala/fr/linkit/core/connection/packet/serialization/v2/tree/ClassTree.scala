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

package fr.linkit.core.connection.packet.serialization.v2.tree

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ClassTree {

    private val userFactories    = ListBuffer.empty[NodeFactory[_]]
    private val defaultFactories = ListBuffer.empty[NodeFactory[_]]
    private val descriptions     = new mutable.HashMap[Class[_], SerializableClassDescription]

    def getNodeForClass[T](clazz: Class[_], parent: SerialNode[_] = null): SerialNode[T] = {
        userFactories
                .find(_.canHandle(clazz))
                .getOrElse(getDefaultFactory(clazz))
                .asInstanceOf[NodeFactory[T]]
                .newNode(this, getDesc(clazz), parent)
    }

    def getSerialNodeForRef[T](any: T): SerialNode[T] = {
        getNodeForClass[T](any.getClass.asInstanceOf[Class[_]])
    }

    def attachFactory(factory: NodeFactory[_]): Unit = {
        userFactories += factory
    }

    def listNodes(clazz: Class[_], obj: Any, parent: SerialNode[_]): List[SerialNode[_]] = {
        listNodes(getDesc(clazz), obj, parent)
    }

    def listNodes(desc: SerializableClassDescription, obj: Any, parent: SerialNode[_]): List[SerialNode[_]] = {
        val fields = desc.serializableFields
        fields.map(field => getNodeForClass(field.get(obj).getClass, parent))
    }

    def getDesc(clazz: Class[_]): SerializableClassDescription = {
        descriptions.getOrElseUpdate(clazz, new SerializableClassDescription(clazz))
    }

    def getDeserialNodeFor(bytes: Array[Byte], parent: DeserialNode[_] = null): DeserialNode[_] = {
        userFactories.find(_.canHandle(bytes))
                .getOrElse(getDefaultFactory(bytes))
                .newNode(this, bytes, parent)
    }

    private def getDefaultFactory[T](clazz: Class[T]): NodeFactory[T] = {
        defaultFactories.find(_.canHandle(clazz))
                .get
                .asInstanceOf[NodeFactory[T]]
    }

    private def getDefaultFactory[T](bytes: Array[Byte]): NodeFactory[T] = {
         defaultFactories.find(_.canHandle(bytes))
                .get
                .asInstanceOf[NodeFactory[T]]
    }

    //The order of registration have an effect.
    defaultFactories += ArrayNode
    defaultFactories += PrimitiveNode.apply
    defaultFactories += ObjectNode.apply
    defaultFactories += EnumNode.apply
    defaultFactories += StringNode

}

object ClassTree {

    implicit class MegaByte(self: Byte) {

        def /\(bytes: Array[Byte]): Array[Byte] = {
            Array(self) ++ bytes
        }

        def /\(other: Byte): Array[Byte] = {
            other /\ Array(other)
        }
    }

}
