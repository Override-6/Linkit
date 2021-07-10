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

import fr.linkit.api.connection.packet.serialization.tree._
import fr.linkit.engine.connection.packet.serialization.tree.DefaultSerialContext.PacketClassNameRequest
import fr.linkit.engine.connection.packet.serialization.tree.nodes._
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.RequestPacketChannel
import fr.linkit.engine.local.mapping.ClassMappings
import fr.linkit.engine.local.utils.{NumberSerializer, ScalaUtils}

import scala.reflect.ClassTag

class EngineNodeFinder(channel: RequestPacketChannel, engineId: String, context: DefaultSerialContext) extends NodeFinder {

    private val engineScope = ChannelScopes.retains(engineId)

    import context._

    override def getSerialNodeForType[T](clazz: Class[_]): SerialNode[T] = {
        userFactories
                .find(_.canHandle(clazz))
                .getOrElse(getDefaultFactory(clazz))
                .asInstanceOf[NodeFactory[T]]
                .newNode(this, getClassProfile[T](clazz.asInstanceOf[Class[T]]))
    }

    override def getSerialNodeForRef[T: ClassTag](any: T): SerialNode[T] = {
        if (any == null)
            return NullNode
                    .newNode(this, getProfile[Null])
                    .asInstanceOf[SerialNode[T]]
        getSerialNodeForType[T](any.getClass.asInstanceOf[Class[_]])
    }

    override def getProfile[T: ClassTag]: ClassProfile[T] = context.getProfile[T]

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

    override def getDeserialNodeFor[T](bytes: Array[Byte]): DeserialNode[T] = {
        val seq = DefaultByteSeq(bytes)

        def findFactory(): Option[NodeFactory[_]] = {
            userFactories.find(_.canHandle(seq))
                    .orElse(getDefaultFactory(seq))
        }

        def noFactory(tryMessage: String): Nothing = {
            throw new FactoryNotFoundException(s"No factory found for bytes '${ScalaUtils.toPresentableString(bytes)}': $tryMessage")
        }

        findFactory()
                .getOrElse {
                    if (seq.isClassDefined) //should not happen because ObjectNode's factory always accept bytes sequences as long as they contains a class.
                        noFactory(s"Factory not found for class ${seq.clazz.get}")
                    else {
                        val name = channel.makeRequest(engineScope)
                                .addPacket(PacketClassNameRequest(NumberSerializer.deserializeInt(seq, 0), null))
                                .submit()
                                .nextResponse
                                .nextPacket[PacketClassNameRequest]
                                .name
                        if (name == null)
                            noFactory("Tried to retrieve a class CONTINUER") //TODO continuer le message
                        ClassMappings.putClass(Class.forName(name))
                        findFactory().getOrElse(noFactory("CONTINUER")) //TODO ici aussi
                    }
                }
                .asInstanceOf[NodeFactory[T]]
                .newNode(this, seq)
    }

    private def getDefaultFactory[T](clazz: Class[T]): NodeFactory[T] = {
        defaultFactories.find(_.canHandle(clazz))
                .getOrElse(throw new NoSuchElementException(s"Could not find factory for '$clazz'"))
                .asInstanceOf[NodeFactory[T]]
    }

    private def getDefaultFactory[T](info: DefaultByteSeq): Option[NodeFactory[T]] = {
        defaultFactories.find(_.canHandle(info))
                .asInstanceOf[Option[NodeFactory[T]]]
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
