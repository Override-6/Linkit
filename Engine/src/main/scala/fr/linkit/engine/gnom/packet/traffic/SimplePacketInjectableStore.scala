/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet.traffic

import java.io.Closeable

import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.traffic._
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.obj.{TrafficObjectReference, TrafficReference}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.api.internal.system.{JustifiedCloseable, Reason}

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class SimplePacketInjectableStore(traffic: PacketTraffic,
                                  tnol: TrafficNetworkObjectLinker,
                                  override val defaultPersistenceConfig: PersistenceConfig,
                                  override val trafficPath: Array[Int])
    extends PacketInjectableStore with InternalPacketInjectableStore with JustifiedCloseable {

    override val reference: TrafficObjectReference = new TrafficObjectReference(trafficPath)
    override val presence : NetworkObjectPresence  = tnol.getPresence(reference)
    private  val children                          = mutable.HashMap.empty[Int, TrafficNode[_]]
    private var closed    : Boolean                = false

    override def getInjectable[C <: PacketInjectable : ClassTag](id: Int, config: PersistenceConfig, factory: PacketInjectableFactory[C], scopeFactory: ChannelScope.ScopeFactory[_ <: ChannelScope]): TrafficNode[C] = {
        val childPath = trafficPath :+ id

        val nodeOpt = children.get(id)
        if (nodeOpt.isDefined) {
            val clazz                = classTag[C].runtimeClass
            val node: TrafficNode[_] = nodeOpt.get
            val injectable           = node.injectable
            val presenceClass        = injectable.getClass
            node match {
                case node: TrafficNode[C] if clazz.isAssignableFrom(presenceClass) => return node
                case o                                                             =>
                    throw new ConflictException(s"Could not return current packet injectable: A PacketInjectable of type '${o.getClass.getSimpleName} exists at @traffic/$id, which is not assignable to request type '${clazz.getSimpleName}'")
            }
        }

        val scope = scopeFactory(traffic.newWriter(childPath, config))
        completeCreation(id, config, scope, factory)
    }

    @inline
    private def completeCreation[C <: PacketInjectable](id: Int, config: PersistenceConfig, scope: ChannelScope, factory: PacketInjectableFactory[C]): TrafficNode[C] = {
        val injectable = factory.createNew(this, scope)
        tnol.registerReference(injectable.reference)
        val node = SimpleTrafficNode(injectable, config, traffic)
        children.put(id, node)
        node
    }

    override def getPersistenceConfig(path: Array[Int], pos: Int): PersistenceConfig = {
        val len = path.length
        if (pos >= len) {
            return failPresence(classOf[PersistenceConfig], path)
        }
        children.get(path(pos)) match {
            case None       => defaultPersistenceConfig
            case Some(node) => node.injectable match {
                case _: PacketInjectable                  => node.persistenceConfig
                case store: InternalPacketInjectableStore =>
                    if (pos - len == 1) node.persistenceConfig //no more sub item in path: the targeted presence is the store.
                    else store.getPersistenceConfig(path, pos + 1) //else, path iteration is not complete, continue.
            }
        }
    }

    override def findNode(path: Array[Int], pos: Int): Option[TrafficNode[PacketInjectable]] = {
        val len = path.length
        if (pos >= len) {
            return None//failPresence(classOf[TrafficObject[TrafficReference]], path)
        }
        children.get(path(pos)).flatMap {
            case node@SimpleTrafficNode(_: PacketInjectable, _, _)                  =>
                if (pos == len - 1)
                    Some(node.asInstanceOf[TrafficNode[PacketInjectable]])
                else None
            case node@SimpleTrafficNode(store: InternalPacketInjectableStore, _, _) =>
                if (pos - len == 1)
                    Some(node.asInstanceOf[TrafficNode[PacketInjectable]]) //no more sub item in path: the targeted presence is this store.
                else store.findNode(path, pos + 1) //else, path iteration is not complete, continue.
        }
    }

    private def failPresence(kind: Class[_], path: Array[Int]): Nothing = {
        throw new NoSuchTrafficPresenceException(s"Could not find ${kind.getSimpleName} at '${TrafficReference / path}'.")
    }

    override def close(cause: Reason): Unit = {
        children.values.foreach {
            case SimpleTrafficNode(closeable: Closeable, _, _) => closeable.close()
            case _                                             => //not closeable ? don't close.
        }
        closed = true
    }

    override def findStore(id: Int): Option[PacketInjectableStore] = children.get(id).flatMap {
        case SimpleTrafficNode(store: PacketInjectableStore, _, _) => Some(store)
        case _                                                     => None
    }

    override def createStore(id: Int, persistenceConfig: PersistenceConfig): PacketInjectableStore = {
        if (children.contains(id)) {
            val msg = s"already present at @traffic/${(trafficPath :+ id).mkString("/")}"
            children(id) match {
                case SimpleTrafficNode(_: PacketInjectableStore, _, _) =>
                    throw new ConflictException(s"PacketInjectableStore $msg")
                case SimpleTrafficNode(i: PacketInjectable, _, _)      =>
                    throw new ConflictException(s"PacketInjectable (of type ${i.getClass.getName}) $msg")
            }
        }
        val store = new SimplePacketInjectableStore(traffic, tnol, persistenceConfig, trafficPath :+ id)
        children.put(id, SimpleTrafficNode(store, persistenceConfig, traffic))
        store
    }

    override def findInjectable[C <: PacketInjectable : ClassTag](id: Int): Option[C] = children.get(id).flatMap {
        case SimpleTrafficNode(value: C, _, _) if value.getClass.isAssignableFrom(classTag[C].runtimeClass) => Some(value)
        case _                                                                                              => None
    }

    override def isClosed: Boolean = closed
}

