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

package fr.linkit.engine.connection.packet.traffic

import fr.linkit.api.connection.packet.channel.ChannelScope
import fr.linkit.api.connection.packet.persistence.context.PersistenceConfig
import fr.linkit.api.connection.packet.traffic._
import fr.linkit.api.connection.packet.traffic.injection.PacketInjectionController
import fr.linkit.api.local.system.{JustifiedCloseable, Reason}
import java.io.Closeable

import scala.annotation.tailrec
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class SimplePacketInjectableStore(traffic: PacketTraffic,
                                  override val defaultPersistenceConfig: PersistenceConfig,
                                  override val path: Array[Int]) extends PacketInjectableStore with JustifiedCloseable with TrafficPresence {

    private val presences       = mutable.HashMap.empty[Int, (TrafficPresence, PersistenceConfig)]
    private var closed: Boolean = false

    override def getInjectable[C <: PacketInjectable : ClassTag](id: Int, config: PersistenceConfig, factory: PacketInjectableFactory[C], scopeFactory: ChannelScope.ScopeFactory[_ <: ChannelScope]): C = {
        val childPath = path :+ id

        val presenceOpt = presences.get(id)
        if (presenceOpt.isDefined) {
            val clazz    = classTag[C].runtimeClass
            val presence = presenceOpt.get._1
            presence match {
                case injectable: C if injectable.getClass eq clazz => return injectable
                case _                                             =>
                    throw new ConflictException("This scope can conflict with other scopes that are registered within this injectable identifier.")
            }
        }

        val scope = scopeFactory(traffic.newWriter(childPath, config))
        completeCreation(id, config, scope, factory)
    }

    @inline
    private def completeCreation[C <: PacketInjectable](id: Int, config: PersistenceConfig, scope: ChannelScope, factory: PacketInjectableFactory[C]): C = {
        val injectable = factory.createNew(this, scope)
        presences.put(id, (injectable, config))
        injectable
    }

    def getPersistenceConfig(path: Array[Int]): PersistenceConfig = {
        getPersistenceConfig(path, 0)
    }

    @tailrec
    private def getPersistenceConfig(path: Array[Int], pos: Int): PersistenceConfig = {
        val len = path.length
        if (pos >= len) {
            return failPresence(path)
        }
        presences.get(path(pos)) match {
            case None                     => defaultPersistenceConfig
            case Some((presence, config)) => presence match {
                case _: PacketInjectable                => config
                case store: SimplePacketInjectableStore =>
                    if (pos - len == 1) config //no more sub item in path: the targeted presence is the store.
                    else store.getPersistenceConfig(path, pos + 1) //else, path iteration is not complete, continue.
            }
        }
    }

    private def failPresence(path: Array[Int]): Nothing = {
        throw new NoSuchTrafficPresenceException(s"Could not find TrafficPresence at path ${path.mkString("/")}.")
    }

    def inject(injection: PacketInjectionController): Unit = {
        if (!injection.haveMoreIdentifier)
            failPresence(injection.injectablePath)
        presences.get(injection.nextIdentifier) match {
            case None        => failPresence(injection.injectablePath)
            case Some(value) => value._1 match {
                case injectable: PacketInjectable       => injection.process(injectable)
                case store: SimplePacketInjectableStore => store.inject(injection)
            }
        }
    }

    override def close(cause: Reason): Unit = {
        presences.values.foreach {
            case (closeable: JustifiedCloseable, _) => closeable.close(return)
            case (closeable: Closeable, _)          => closeable
            case _                                  => //not closeable ? don't close.
        }
        closed = true
    }

    override def findStore(id: Int): Option[PacketInjectableStore] = presences.get(id).flatMap {
        case (store: SimplePacketInjectableStore, _) => Some(store)
        case _                                       => None
    }

    override def createStore(id: Int, persistenceConfig: PersistenceConfig): PacketInjectableStore = {
        if (presences.contains(id))
            throw new ConflictException(s"PacketInjectableStore already created at ${path.mkString("/")}/$id")
        val store = new SimplePacketInjectableStore(traffic, persistenceConfig, path :+ id)
        presences.put(id, (store, persistenceConfig))
        store
    }

    override def isClosed: Boolean = closed
}

