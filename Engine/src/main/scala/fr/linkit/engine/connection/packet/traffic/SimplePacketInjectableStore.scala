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
import fr.linkit.api.connection.packet.traffic._
import fr.linkit.api.local.system.{JustifiedCloseable, Reason}

import java.io.Closeable
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class SimplePacketInjectableStore(traffic: PacketTraffic,
                                  override val path: Array[Int]) extends PacketInjectableStore with JustifiedCloseable with TrafficPresence {

    private val presences       = mutable.HashMap.empty[Int, TrafficPresence]
    private var closed: Boolean = false

    override def getInjectable[C <: PacketInjectable : ClassTag](id: Int, factory: PacketInjectableFactory[C], scopeFactory: ChannelScope.ScopeFactory[_ <: ChannelScope]): C = {
        val childPath = path :+ id

        val presenceOpt = presences.get(id)
        if (presenceOpt.isDefined) {
            val clazz    = classTag[C].runtimeClass
            val presence = presenceOpt.get
            presence match {
                case injectable: C if injectable.getClass eq clazz => return injectable
                case _                                             =>
                    throw new ConflictException("This scope can conflict with other scopes that are registered within this injectable identifier.")
            }
        }

        val scope = scopeFactory(traffic.newWriter(childPath))
        completeCreation(id, scope, factory)
    }

    @inline
    private def completeCreation[C <: PacketInjectable](id: Int, scope: ChannelScope, factory: PacketInjectableFactory[C]): C = {
        val injectable = factory.createNew(this, scope)
        presences.put(id, injectable)
        injectable
    }

    def inject(injection: PacketInjection): Unit = {
        val bundle = injection.bundle

        def fail(): Nothing = {
            val path = bundle.coords.path
            throw new NoSuchTrafficPresenceException(s"Could not find TrafficPresence at path ${path.mkString("/")}.")
        }

        if (!injection.haveMoreIdentifier)
            fail()
        presences.get(injection.nextIdentifier) match {
            case None        => fail()
            case Some(value) => value match {
                case injectable: PacketInjectable       => injectable.inject(bundle)
                case store: SimplePacketInjectableStore => store.inject(injection)
            }
        }
    }

    override def close(cause: Reason): Unit = {
        presences.values.foreach {
            case closeable: JustifiedCloseable => closeable.close(return)
            case closeable: Closeable          => closeable
            case _                             => //not closeable ? don't close.
        }
        closed = true
    }

    override def findStore(id: Int): Option[PacketInjectableStore] = presences.get(id).flatMap {
        case store: SimplePacketInjectableStore => Some(store)
        case _                                  => None
    }

    override def createStore(id: Int): PacketInjectableStore = {
        if (presences.contains(id))
            throw new ConflictException(s"PacketInjectableStore already created at ${path.mkString("/")}/$id")
        val store = new SimplePacketInjectableStore(traffic, path :+ id)
        presences.put(id, store)
        store
    }

    override def isClosed: Boolean = closed
}

