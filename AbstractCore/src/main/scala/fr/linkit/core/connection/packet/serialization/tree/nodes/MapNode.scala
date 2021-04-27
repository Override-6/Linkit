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

package fr.linkit.core.connection.packet.serialization.tree.nodes

import fr.linkit.core.connection.packet.serialization.tree._
import fr.linkit.core.local.mapping.ClassMappings
import fr.linkit.core.local.utils.{NumberSerializer, ScalaUtils}

import scala.collection.mutable.ArrayBuffer
import scala.collection.{MapFactory, mutable}

object MapNode {

    def ofMutable: NodeFactory[mutable.Map[_, _]] = new NodeFactory[mutable.Map[_, _]] {
        override def canHandle(clazz: Class[_]): Boolean = {
            classOf[mutable.Map[_, _]].isAssignableFrom(clazz)
        }

        override def canHandle(bytes: Array[Byte]): Boolean = {
            if (bytes.length < 4)
                return false
            val clazzInt = NumberSerializer.deserializeInt(bytes, 0)
            ClassMappings.getClassOpt(clazzInt).exists(findFactory(_).isDefined)
        }

        override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[mutable.Map[_, _]] = {
            new MutableMapSerialNode(parent, finder)
        }

        override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[mutable.Map[_, _]] = {
            new MutableMapDeserialNode(parent, bytes, finder)
        }
    }

    def ofImmutable: NodeFactory[Map[_, _]] = new NodeFactory[Map[_, _]] {
        override def canHandle(clazz: Class[_]): Boolean = {
            classOf[Map[_, _]].isAssignableFrom(clazz)
        }

        override def canHandle(bytes: Array[Byte]): Boolean = {
            if (bytes.length < 4)
                return false

            val clazzInt = NumberSerializer.deserializeInt(bytes, 0)
            ClassMappings.getClassOpt(clazzInt).exists(findFactory(_).isDefined)
        }

        override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[Map[_, _]] = {
            new ImmutableMapSerialNode(parent, finder)
        }

        override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[Map[_, _]] = {
            new ImmutableMapDeserialNode(parent, bytes, finder)
        }
    }

    class MutableMapSerialNode(override val parent: SerialNode[_], finder: NodeFinder) extends SerialNode[mutable.Map[_, _]] {

        override def serialize(t: mutable.Map[_, _], putTypeHint: Boolean): Array[Byte] = {
            val content      = t.toArray
            val mapTypeBytes = NumberSerializer.serializeInt(t.getClass.getName.hashCode)
            //println(s"content = ${content.mkString("Array(", ", ", ")")}")
            mapTypeBytes ++ finder.getSerialNodeForRef(content).serialize(awfulCast(content), putTypeHint)
        }
    }

    class MutableMapDeserialNode(override val parent: DeserialNode[_], bytes: Array[Byte], finder: NodeFinder) extends DeserialNode[mutable.Map[_, _]] {

        override def deserialize(): mutable.Map[_, _] = {
            val mapType = ClassMappings.getClass(NumberSerializer.deserializeInt(bytes, 0))
            //println(s"mapType = ${mapType}")
            val factory = findFactory(mapType)
            //println(s"factory = ${factory}")
            //println(s"bytes.drop(4) = ${new String(bytes.drop(4))}")
            val content = finder.getDeserialNodeFor[Array[Any]](bytes.drop(4)).deserialize()
            //println(s"content = ${content.mkString("Array(", ", ", ")")}")
            if (content.isEmpty)
                return awfulCast(mapType.getConstructor().newInstance())

            awfulCast(factory.get.from(ArrayBuffer.from(ScalaUtils.slowCopy[(_, _)](content))))
        }
    }

    class ImmutableMapSerialNode(override val parent: SerialNode[_], finder: NodeFinder) extends SerialNode[Map[_, _]] {

        override def serialize(t: Map[_, _], putTypeHint: Boolean): Array[Byte] = {
            val content      = t.toArray
            val mapTypeBytes = NumberSerializer.serializeInt(t.getClass.getName.hashCode)
            mapTypeBytes ++ finder.getSerialNodeForRef(content).serialize(awfulCast(content), putTypeHint)
        }
    }

    class ImmutableMapDeserialNode(override val parent: DeserialNode[_], bytes: Array[Byte], finder: NodeFinder) extends DeserialNode[Map[_, _]] {

        override def deserialize(): Map[_, _] = {
            val mapType = ClassMappings.getClass(NumberSerializer.deserializeInt(bytes, 0))
            val factory = findFactory(mapType)
            val content = finder.getDeserialNodeFor(bytes.drop(4)).deserialize()
            awfulCast(factory.get.from(content))
        }
    }

    private def findFactory[T[_, _]](mapType: Class[_]): Option[MapFactory[T]] = {
        try {
            val companionClass = Class.forName(mapType.getName + "$")
            val companion      = companionClass.getField("MODULE$").get(null)
            companion match {
                case e: MapFactory[T] => Option(e)
                case _                => None
            }
        } catch {
            case _: ClassNotFoundException => None
        }
    }

    private def awfulCast[T](any: Any): T = any.asInstanceOf[T]

}
