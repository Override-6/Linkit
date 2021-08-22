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

package fr.linkit.engine.connection.cache.obj.behavior

import fr.linkit.api.connection.cache.obj.behavior.member.MemberBehaviorFactory
import fr.linkit.api.connection.cache.obj.behavior.{SynchronizedObjectBehavior, SynchronizedObjectBehaviorStore}
import fr.linkit.engine.connection.cache.obj.description.SyncObjectClassDescription

import scala.collection.mutable
import scala.reflect.runtime.universe
import scala.reflect.{ClassTag, classTag}

class SynchronizedObjectDefaultBehaviorCenter(override val factory: MemberBehaviorFactory) extends SynchronizedObjectBehaviorStore {

    private val behaviors = mutable.HashMap.empty[Class[_], SynchronizedObjectBehavior[_]]

    def this(factory: MemberBehaviorFactory, behaviors: Map[Class[_], SynchronizedObjectBehavior[_]]) = {
        this(factory)
        this.behaviors ++= behaviors
    }

    override def get[B <: AnyRef : universe.TypeTag : ClassTag]: SynchronizedObjectBehavior[B] = {
        getFromAnyClass(classTag[B].runtimeClass)
    }

    override def getFromClass[B <: AnyRef](clazz: Class[_]): SynchronizedObjectBehavior[B] = {
        getFromAnyClass[B](clazz)
    }

    private def getFromAnyClass[B <: AnyRef](clazz: Class[_]): SynchronizedObjectBehavior[B] = {
        behaviors.getOrElseUpdate(clazz, DefaultSynchronizedObjectBehavior[B](SyncObjectClassDescription[B](clazz), this, null, null, null))
            .asInstanceOf[SynchronizedObjectBehavior[B]]
    }

    override def findFromClass[B <: AnyRef](clazz: Class[_]): Option[SynchronizedObjectBehavior[B]] = {
        behaviors.get(clazz).asInstanceOf[Option[SynchronizedObjectBehavior[B]]]
    }
}