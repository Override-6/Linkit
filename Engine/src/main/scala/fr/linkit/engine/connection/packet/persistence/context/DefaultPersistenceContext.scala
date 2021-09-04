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

package fr.linkit.engine.connection.packet.persistence.context

import fr.linkit.api.connection.packet.persistence.context.{PersistenceContext, TypeProfile}
import fr.linkit.engine.connection.packet.persistence.context.profile.{Deconstructive, DeconstructiveTypeProfile, UnsafeTypeProfile}

import scala.collection.mutable

class DefaultPersistenceContext extends PersistenceContext {

    private val profiles = mutable.HashMap.empty[Class[_], TypeProfile[_]]

    override def getDefaultProfile[T](clazz: Class[_]): TypeProfile[T] = {
        profiles.getOrElseUpdate(clazz, createProfileOfClass(clazz)).asInstanceOf[TypeProfile[T]]
    }

    private def createProfileOfClass(clazz: Class[_]): TypeProfile[_ <: Any] = {
        var profile: TypeProfile[_ <: Any] = null
        try {
            if (classOf[Deconstructive].isAssignableFrom(clazz))
                profile = new DeconstructiveTypeProfile[Deconstructive](clazz)
            else profile = new UnsafeTypeProfile[Any](clazz)
        } catch {
            case _: NoSuchElementException => profile = new UnsafeTypeProfile[Any](clazz)
        }
        profile
    }
}
