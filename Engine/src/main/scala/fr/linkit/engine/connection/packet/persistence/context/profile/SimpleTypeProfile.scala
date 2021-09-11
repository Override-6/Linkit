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

package fr.linkit.engine.connection.packet.persistence.context.profile

import fr.linkit.api.connection.packet.persistence.context.{ObjectConverter, TypePersistence, TypeProfile}
import fr.linkit.engine.connection.packet.persistence.context.ClassObjectStructure
import fr.linkit.engine.local.utils.ClassMap

class SimpleTypeProfile[T <: AnyRef](override val typeClass: Class[_],
                                     persists: Array[TypePersistence[T]],
                                     convertors: ClassMap[ObjectConverter[T, Any]]) extends TypeProfile[T] {

    override def getDefaultPersistence(t: T): TypePersistence[T] = {
        val structure = ClassObjectStructure(t.getClass)
        var i         = 0
        while (i < persists.length) {
            val persistence = persists(i)
            if (persistence.structure.equals(structure))
                return persistence
            i += 1
        }
        throw new NoSuchElementException(s"Could not find type persistence matching object ${t}.")
    }

    override def getPersistence(args: Array[Any]): TypePersistence[T] = {
        var i   = 0
        val len = persists.length
        while (i < len) {
            val persist = persists(i)
            if (persist.structure.isAssignable(args))
                return persist
            i += 1
        }
        throw new NoSuchElementException(s"No Type Persistence matching with object structure array '${args.mkString("(", ", ", ")")}' has been found")
    }

    override def convertTo(t: T): Any = {
        val clazz = t.getClass
        convertors.get(clazz)
                .getOrElse(throw new NoSuchElementException(s"Could not find converter for converting '${clazz.getName}' to Any"))
                .to(t)
    }

    override def convertFrom(any: Any): T = {
        val clazz = any.getClass
        convertors.get(clazz)
                .getOrElse(throw new NoSuchElementException(s"Could not find converter for converting '${clazz.getName}' to '${typeClass.getName}'"))
                .from(any)
    }
}
