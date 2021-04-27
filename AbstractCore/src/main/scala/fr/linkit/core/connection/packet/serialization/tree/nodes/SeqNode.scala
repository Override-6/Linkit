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

import fr.linkit.core.connection.packet.serialization.tree.{DeserialNode, NodeFactory, NodeFinder, SerialNode, SerializableClassDescription}
import fr.linkit.core.local.mapping.ClassMappings
import fr.linkit.core.local.utils.NumberSerializer

import scala.collection.mutable.ArrayBuffer
import scala.collection.{Seq, SeqFactory, SeqOps, mutable}

object SeqNode {

    def ofMutable: NodeFactory[mutable.Seq[_]] = new NodeFactory[mutable.Seq[_]] {
        override def canHandle(clazz: Class[_]): Boolean = {
            clazz != classOf[::[_]] && clazz != Nil.getClass && classOf[mutable.Seq[_]].isAssignableFrom(clazz)
        }

        override def canHandle(bytes: Array[Byte]): Boolean = {
            if (bytes.length < 4)
                return false
            val clazzInt = NumberSerializer.deserializeInt(bytes, 0)
            ClassMappings.getClassOpt(clazzInt).exists(findFactory(_).isDefined)
        }

        override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[mutable.Seq[_]] = {
            new MutableSeqSerialNode(parent, finder)
        }

        override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[mutable.Seq[_]] = {
            new MutableSeqDeserialNode(parent, bytes, finder)
        }
    }

    def ofImmutable: NodeFactory[Seq[_]] = new NodeFactory[Seq[_]] {
        override def canHandle(clazz: Class[_]): Boolean = {
            clazz != classOf[::[_]] && clazz != Nil.getClass && classOf[Seq[_]].isAssignableFrom(clazz)
        }

        override def canHandle(bytes: Array[Byte]): Boolean = {
            if (bytes.length < 4)
                return false

            val clazzInt = NumberSerializer.deserializeInt(bytes, 0)
            ClassMappings.getClassOpt(clazzInt).exists(findFactory(_).isDefined)
        }

        override def newNode(finder: NodeFinder, desc: SerializableClassDescription, parent: SerialNode[_]): SerialNode[Seq[_]] = {
            new ImmutableSeqSerialNode(parent, finder)
        }

        override def newNode(finder: NodeFinder, bytes: Array[Byte], parent: DeserialNode[_]): DeserialNode[Seq[_]] = {
            new ImmutableSeqDeserialNode(parent, bytes, finder)
        }
    }

    class MutableSeqSerialNode(override val parent: SerialNode[_], finder: NodeFinder) extends SerialNode[mutable.Seq[_]] {

        Nil: Seq[_]

        override def serialize(t: mutable.Seq[_], putTypeHint: Boolean): Array[Byte] = {
            val content      = t.toArray
            val seqTypeBytes = NumberSerializer.serializeInt(t.getClass.getName.hashCode)
            //println(s"content = ${content.mkString("Array(", ", ", ")")}")
            seqTypeBytes ++ finder.getSerialNodeForRef(content).serialize(awfulCast(content), putTypeHint)
        }
    }

    class MutableSeqDeserialNode(override val parent: DeserialNode[_], bytes: Array[Byte], finder: NodeFinder) extends DeserialNode[mutable.Seq[_]] {

        override def deserialize(): mutable.Seq[_] = {
            val seqType = ClassMappings.getClass(NumberSerializer.deserializeInt(bytes, 0))
            //println(s"List type = ${seqType}")
            val factory = findFactory(seqType)
            //println(s"factory = ${factory}")
            //println(s"bytes.drop(4) = ${new String(bytes.drop(4))}")
            val content = finder.getDeserialNodeFor[Array[Any]](bytes.drop(4)).deserialize()
            //println(s"SeqNode: content = ${content.mkString("Array(", ", ", ")")}")
            if (content.isEmpty)
                return awfulCast(seqType.getConstructor().newInstance())

            awfulCast(factory.get.from(ArrayBuffer.from(content)))
        }
    }

    class ImmutableSeqSerialNode(override val parent: SerialNode[_], finder: NodeFinder) extends SerialNode[Seq[_]] {

        override def serialize(t: Seq[_], putTypeHint: Boolean): Array[Byte] = {
            val content      = t.toArray
            val seqTypeBytes = NumberSerializer.serializeInt(t.getClass.getName.hashCode)
            seqTypeBytes ++ finder.getSerialNodeForRef(content).serialize(awfulCast(content), putTypeHint)
        }
    }

    class ImmutableSeqDeserialNode(override val parent: DeserialNode[_], bytes: Array[Byte], finder: NodeFinder) extends DeserialNode[Seq[_]] {

        override def deserialize(): Seq[_] = {
            val seqType = ClassMappings.getClass(NumberSerializer.deserializeInt(bytes, 0))
            val factory = findFactory(seqType)
            val content = finder.getDeserialNodeFor(bytes.drop(4)).deserialize()
            awfulCast(factory.get.from(content))
        }
    }

    private def findFactory[CC[A] <: SeqOps[A, Seq, Seq[A]]](seqType: Class[_]): Option[SeqFactory[CC]] = {
        try {
            val companionClass = Class.forName(seqType.getName + "$")
            val companion      = companionClass.getField("MODULE$").get(null)
            //println(s"companion = ${companion}")
            companion match {
                case e: SeqFactory[CC] => Option(e)
                case _                 => None
            }
        } catch {
            case _: ClassNotFoundException => None
        }
    }

    private def awfulCast[T](any: Any): T = any.asInstanceOf[T]

}
