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

import fr.linkit.api.gnom.packet.channel.ChannelScope
import fr.linkit.api.gnom.packet.traffic._
import fr.linkit.api.gnom.packet.traffic.injection.PacketInjectionControl
import fr.linkit.api.gnom.persistence.context.PersistenceConfig
import fr.linkit.api.gnom.persistence.obj.{TrafficPresenceReference, TrafficReference}
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.api.internal.system.{JustifiedCloseable, Reason}

import java.io.Closeable
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class SimplePacketInjectableStore(traffic: PacketTraffic,
                                  tnol: TrafficNetworkObjectLinker,
                                  override val defaultPersistenceConfig: PersistenceConfig,
                                  override val trafficPath: Array[Int])
        extends PacketInjectableStore with InternalPacketInjectableStore with JustifiedCloseable {

    override val reference: TrafficPresenceReference = new TrafficPresenceReference(trafficPath)
    override val presence : NetworkObjectPresence    = tnol.getPresence(reference)
    private  val children                            = mutable.HashMap.empty[Int, (TrafficPresence[TrafficReference], PersistenceConfig)]
    private var closed    : Boolean                  = false

    override def getInjectable[C <: PacketInjectable : ClassTag](id: Int, config: PersistenceConfig, factory: PacketInjectableFactory[C], scopeFactory: ChannelScope.ScopeFactory[_ <: ChannelScope]): C = {
        val childPath = trafficPath :+ id

        val presenceOpt = children.get(id)
        if (presenceOpt.isDefined) {
            val clazz         = classTag[C].runtimeClass
            val presence      = presenceOpt.get._1
            val presenceClass = presence.getClass
            presence match {
                case injectable: C if clazz.isAssignableFrom(presenceClass) => return injectable
                case o                                                      =>
                    throw new ConflictException(s"Could not return current packet injectable: A PacketInjectable of type '${o.getClass.getSimpleName} exists at @traffic/$id, which is not assignable to request type '${clazz.getSimpleName}'")
            }
        }

        val scope = scopeFactory(traffic.newWriter(childPath, config))
        completeCreation(id, config, scope, factory)
    }

    @inline
    private def completeCreation[C <: PacketInjectable](id: Int, config: PersistenceConfig, scope: ChannelScope, factory: PacketInjectableFactory[C]): C = {
        val injectable = factory.createNew(this, scope)
        tnol.registerReference(injectable.reference)
        children.put(id, (injectable, config))
        injectable
    }

    override def getPersistenceConfig(path: Array[Int], pos: Int): PersistenceConfig = {
        val len = path.length
        if (pos >= len) {
            return failPresence(classOf[PersistenceConfig], path)
        }
        children.get(path(pos)) match {
            case None                     => defaultPersistenceConfig
            case Some((presence, config)) => presence match {
                case _: PacketInjectable                  => config
                case store: InternalPacketInjectableStore =>
                    if (pos - len == 1) config //no more sub item in path: the targeted presence is the store.
                    else store.getPersistenceConfig(path, pos + 1) //else, path iteration is not complete, continue.
            }
        }
    }

    override def findPresence(path: Array[Int], pos: Int): Option[TrafficPresence[TrafficReference]] = {
        val len = path.length
        if (pos >= len) {
            failPresence(classOf[TrafficPresence[TrafficReference]], path)
        }
        children.get(path(pos)).flatMap {
            case (injectable: PacketInjectable, _)         =>
                if (pos == len - 1)
                    Some(injectable)
                else None
            case (store: InternalPacketInjectableStore, _) =>
                if (pos - len == 1) Some(this) //no more sub item in path: the targeted presence is this store.
                else store.findPresence(path, pos + 1) //else, path iteration is not complete, continue.
        }
    }

    private def failPresence(kind: Class[_], path: Array[Int]): Nothing = {
        throw new NoSuchTrafficPresenceException(s"Could not find ${kind.getSimpleName} at path ${path.mkString("/")}.")
    }

    override def inject(injection: PacketInjectionControl): Unit = {
        if (!injection.haveMoreIdentifier)
            failPresence(classOf[PacketInjectable], injection.injectablePath)
        children.get(injection.nextIdentifier) match {
            case Some(value) => value._1 match {
                case injectable: PacketInjectable         => injection.process(injectable)
                case store: InternalPacketInjectableStore => store.inject(injection)
            }
            case None        => failPresence(classOf[PacketInjectable], injection.injectablePath)
        }
    }

    override def close(cause: Reason): Unit = {
        children.values.foreach {
            case (closeable: JustifiedCloseable, _) => closeable.close(return)
            case (closeable: Closeable, _)          => closeable
            case _                                  => //not closeable ? don't close.
        }
        closed = true
    }

    override def findStore(id: Int): Option[PacketInjectableStore] = children.get(id).flatMap {
        case (store: PacketInjectableStore, _) => Some(store)
        case _                                 => None
    }

    override def createStore(id: Int, persistenceConfig: PersistenceConfig): PacketInjectableStore = {
        if (children.contains(id)) {
            val msg = s"already present at @traffic/${(trafficPath :+ id).mkString("/")}"
            children(id) match {
                case (_: PacketInjectableStore, _) =>
                    throw new ConflictException(s"PacketInjectableStore $msg")
                case (i: PacketInjectable, _)      =>
                    throw new ConflictException(s"PacketInjectable (of type ${i.getClass.getName}) $msg")
            }
        }
        val store = new SimplePacketInjectableStore(traffic, tnol, persistenceConfig, trafficPath :+ id)
        children.put(id, (store, persistenceConfig))
        store
    }

    override def findInjectable[C <: PacketInjectable : ClassTag](id: Int): Option[C] = children.get(id).flatMap {
        case (value: C, _) if value.getClass.isAssignableFrom(classTag[C].runtimeClass) => Some(value)
        case _                                                                          => None
    }

    override def isClosed: Boolean = closed
}

