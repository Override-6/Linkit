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

package fr.linkit.engine.connection.packet.persistence.tree.nodes

import fr.linkit.api.connection.packet.persistence.tree._
import fr.linkit.engine.connection.packet.persistence.tree._
import fr.linkit.engine.local.mapping.ClassMappings
import fr.linkit.engine.local.utils.{NumberSerializer, ScalaUtils}

import scala.collection.mutable.ArrayBuffer
import scala.collection.{MapFactory, mutable}

object MapNode {

    def ofMutable: NodeFactory[mutable.Map[_, _]] = new NodeFactory[mutable.Map[_, _]] {
        override def canHandle(clazz: Class[_]): Boolean = {
            classOf[mutable.Map[_, _]].isAssignableFrom(clazz)
        }

        override def canHandle(bytes: ByteSeq): Boolean = {
            bytes.classExists(cl => classOf[mutable.Map[_, _]].isAssignableFrom(cl) && findFactory[mutable.Map](cl).isDefined)
        }

        override def newNode(finder: NodeFinder, profile: ClassProfile[mutable.Map[_, _]]): SerialNode[mutable.Map[_, _]] = {
            new MutableMapSerialNode(profile, finder)
        }

        override def newNode(finder: NodeFinder, bytes: ByteSeq): DeserialNode[mutable.Map[_, _]] = {
            new MutableMapDeserialNode(finder.getClassProfile(bytes.getClassOfSeq), bytes, finder)
        }
    }

    def ofImmutable: NodeFactory[Map[_, _]] = new NodeFactory[Map[_, _]] {
        override def canHandle(clazz: Class[_]): Boolean = {
            classOf[Map[_, _]].isAssignableFrom(clazz)
        }

        override def canHandle(bytes: ByteSeq): Boolean = {
            bytes.classExists(cl => classOf[Map[_, _]].isAssignableFrom(cl) && findFactory[Map](cl).isDefined)
        }

        override def newNode(finder: NodeFinder, profile: ClassProfile[Map[_, _]]): SerialNode[Map[_, _]] = {
            new ImmutableMapSerialNode(profile, finder)
        }

        override def newNode(finder: NodeFinder, bytes: ByteSeq): DeserialNode[Map[_, _]] = {
            new ImmutableMapDeserialNode(finder.getClassProfile(bytes.getClassOfSeq), bytes, finder)
        }
    }

    class MutableMapSerialNode(profile: ClassProfile[mutable.Map[_, _]], finder: NodeFinder) extends SerialNode[mutable.Map[_, _]] {

        override def serialize(t: mutable.Map[_, _], putTypeHint: Boolean): Array[Byte] = {
            profile.applyAllSerialProcedures(t)
            val content      = t.toArray
            val mapTypeBytes = NumberSerializer.serializeInt(t.getClass.getName.hashCode)
            //println(s"content = ${content.mkString("Array(", ", ", ")")}")
            mapTypeBytes ++ finder.getSerialNodeForRef(content).serialize(awfulCast(content), putTypeHint)
        }
    }

    class MutableMapDeserialNode(profile: ClassProfile[mutable.Map[_, _]], bytes: Array[Byte], finder: NodeFinder) extends DeserialNode[mutable.Map[_, _]] {

        override def deserialize(): mutable.Map[_, _] = {
            val mapType = ClassMappings.getClass(NumberSerializer.deserializeInt(bytes, 0))
            //println(s"mapType = ${mapType}")
            val factory = findFactory[mutable.Map](mapType)
            //println(s"factory = ${factory}")
            //println(s"bytes.drop(4) = ${new String(bytes.drop(4))}")
            val content = finder.getDeserialNodeFor[Array[Any]](bytes.drop(4)).deserialize()
            //println(s"content = ${content.mkString("Array(", ", ", ")")}")
            if (content.isEmpty)
                return awfulCast(mapType.getConstructor().newInstance())

            val result = awfulCast[mutable.Map[_, _]](factory.get.from(ArrayBuffer.from(ScalaUtils.slowCopy[(_, _)](content))))
            profile.applyAllDeserialProcedures(result)
            result
        }
    }

    class ImmutableMapSerialNode(profile: ClassProfile[Map[_, _]], finder: NodeFinder) extends SerialNode[Map[_, _]] {

        override def serialize(t: Map[_, _], putTypeHint: Boolean): Array[Byte] = {
            profile.applyAllSerialProcedures(t)
            val content      = t.toArray
            val mapTypeBytes = NumberSerializer.serializeInt(t.getClass.getName.hashCode)
            mapTypeBytes ++ finder.getSerialNodeForRef(content).serialize(awfulCast(content), putTypeHint)
        }
    }

    class ImmutableMapDeserialNode(profile: ClassProfile[Map[_, _]], bytes: Array[Byte], finder: NodeFinder) extends DeserialNode[Map[_, _]] {

        override def deserialize(): Map[_, _] = {
            val mapType = ClassMappings.getClass(NumberSerializer.deserializeInt(bytes, 0))
            val factory = findFactory[Map](mapType)
            val content = finder.getDeserialNodeFor(bytes.drop(4)).deserialize()
            val result  = awfulCast[Map[_, _]](factory.get.from(content))
            profile.applyAllDeserialProcedures(result)
            result
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
