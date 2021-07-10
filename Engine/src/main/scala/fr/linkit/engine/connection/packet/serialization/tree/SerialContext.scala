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

package fr.linkit.engine.connection.packet.serialization.tree

import fr.linkit.api.connection.network.Network
import fr.linkit.engine.connection.packet.serialization.procedure.Procedure
import fr.linkit.engine.connection.packet.serialization.tree.SerialContext.ClassProfile
import fr.linkit.engine.connection.packet.serialization.tree.nodes._
import fr.linkit.engine.local.utils.ScalaUtils
import org.jetbrains.annotations.Nullable

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}

class SerialContext() extends ContextHolder {

    private val userFactories              = ListBuffer.empty[NodeFactory[_]]
    private val defaultFactories           = ListBuffer.empty[NodeFactory[_]]
    private val profiles                   = new mutable.HashMap[Class[_], ClassProfile[_]]()
    @Nullable private var network: Network = _

    override def attachProcedure[C: ClassTag](procedure: Procedure[C]): Unit = {
        getProfile[C].addProcedure(procedure)
    }

    override def detachProcedure[C: ClassTag](procedure: Procedure[C]): Unit = {
        getProfile[C].removeProcedure(procedure)
    }

    override def detachFactory(nodeFactory: NodeFactory[_]): Unit = {
        userFactories -= nodeFactory
    }

    override def attachFactory(factory: NodeFactory[_]): Unit = {
        userFactories += factory
    }

    def updateNetwork(network: Network): Unit = {
        if (network == null)
            throw new IllegalArgumentException
        this.network = network
    }

    @Nullable
    def getNetwork: Network = network

    def getSerialNodeForType[T](clazz: Class[_]): SerialNode[T] = {
        userFactories
                .find(_.canHandle(clazz))
                .getOrElse(getDefaultFactory(clazz))
                .asInstanceOf[NodeFactory[T]]
                .newNode(this, getClassProfile[T](clazz.asInstanceOf[Class[T]]))
    }

    def getSerialNodeForRef[T: ClassTag](any: T): SerialNode[T] = {
        if (any == null)
            return NullNode
                    .newNode(this, getProfile[Null])
                    .asInstanceOf[SerialNode[T]]
        getSerialNodeForType[T](any.getClass.asInstanceOf[Class[_]])
    }

    def listNodes[T](clazz: Class[T], obj: T): List[SerialNode[_]] = {
        listNodes(getClassProfile[T](clazz), obj)
    }

    def listNodes[T](profile: ClassProfile[T], obj: T): List[SerialNode[_]] = {
        val fields = profile.desc.serializableFields
        fields.map(field => {
            val fieldValue = field.get(obj)
            if (fieldValue == null)
                getSerialNodeForRef(null)
            else
                getSerialNodeForType(fieldValue.getClass)
        })
    }

    def getDeserialNodeFor[T](bytes: Array[Byte]): DeserialNode[T] = {
        val seq = ByteSeq(bytes)
        userFactories.find(_.canHandle(seq))
                .getOrElse(getDefaultFactory(seq))
                .asInstanceOf[NodeFactory[T]]
                .newNode(this, seq)
    }

    def getProfile[T: ClassTag]: ClassProfile[T] = {
        getClassProfile(classTag[T]
                .runtimeClass
                .asInstanceOf[Class[T]])
    }

    def getClassProfile[T](clazz: Class[_ <: T]): ClassProfile[T] = {
        //println(s"clazz = ${clazz}")
        profiles.getOrElseUpdate(clazz, new ClassProfile(clazz, this))
                .asInstanceOf[ClassProfile[T]]
    }

    private def getDefaultFactory[T](clazz: Class[T]): NodeFactory[T] = {
        defaultFactories.find(_.canHandle(clazz))
                .get
                .asInstanceOf[NodeFactory[T]]
    }

    private def getDefaultFactory[T](info: ByteSeq): NodeFactory[T] = {
        defaultFactories.find(_.canHandle(info))
                .getOrElse(throw new NoSuchElementException(s"Could not find factory for bytes '${ScalaUtils.toPresentableString(info.array)}'"))
                .asInstanceOf[NodeFactory[T]]
    }

    //The order of registration have an effect.
    defaultFactories += NullNode
    defaultFactories += ArrayNode
    defaultFactories += StringNode
    defaultFactories += EnumNode.apply
    defaultFactories += SeqNode.ofMutable
    defaultFactories += SeqNode.ofImmutable
    defaultFactories += MapNode.ofMutable
    defaultFactories += MapNode.ofImmutable
    defaultFactories += PrimitiveNode.apply
    defaultFactories += DateNode
    defaultFactories += ObjectNode.apply

}

object SerialContext {

    implicit class MegaByte(self: Byte) {

        def /\(bytes: Array[Byte]): Array[Byte] = {
            Array(self) ++ bytes
        }

        def /\(other: Byte): Array[Byte] = {
            self /\ Array(other)
        }
    }

    class ClassProfile[T](clazz: Class[T], context: SerialContext) {

        val desc = new SerializableClassDescription(clazz)
        private val procedures = ListBuffer.empty[Procedure[T]]

        def addProcedure(procedure: Procedure[T]): Unit = procedures += procedure

        def removeProcedure(procedure: Procedure[T]): Unit = procedures -= procedure

        def applyAllSerialProcedures(t: T): Unit = {
            applyAllProcedures(t, _.beforeSerial)
        }

        def applyAllDeserialProcedures(t: T): Unit = {
            applyAllProcedures(t, _.afterDeserial)
        }

        private def applyAllProcedures(t: T, action: Procedure[_ >: T] => (T, Network) => Unit): Unit = {
            val network = context.network
            if (t == null || network == null)
                return
            procedures.foreach(action(_)(t, network))
            val superClass = clazz.getSuperclass
            if (superClass == null)
                return
            context.getClassProfile(superClass)
                    .procedures
                    .foreach(action(_)(t, network))

            applyUpperInterfaceProcedures(clazz)

            def applyUpperInterfaceProcedures(clazz: Class[_ >: T]): Unit = {
                clazz.getInterfaces
                        .map(_.asInstanceOf[Class[_ >: T]])
                        .foreach(interface => {
                            context.getClassProfile(interface)
                                    .procedures
                                    .foreach(action(_)(t, network))
                            applyUpperInterfaceProcedures(interface)
                        })
            }
        }
    }

}
