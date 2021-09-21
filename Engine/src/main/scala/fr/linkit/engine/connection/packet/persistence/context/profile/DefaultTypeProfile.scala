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

import fr.linkit.api.connection.packet.persistence.context.{TypePersistence, TypeProfile}
import fr.linkit.engine.connection.packet.persistence.context.structure.ClassObjectStructure
import fr.linkit.engine.local.utils.ClassMap

class DefaultTypeProfile[T <: AnyRef](override val typeClass: Class[_],
                                      override val declaredParent: TypeProfile[_ >: T],
                                      private[context] val persists: Array[TypePersistence[T]]) extends TypeProfile[T] {

    override def getPersistence(t: T): TypePersistence[T] = {
        return persists.head
        //TODO Choose between other compatible persistence (if an error occured or...)
        if (declaredParent ne null)
            declaredParent.getPersistence(t)
        else
            throw new NoSuchElementException(s"Could not find type persistence matching object ${t} (of class ${t.getClass.getName}.")
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
        if (declaredParent ne null)
            declaredParent.getPersistence(args)
        else
            throw new NoSuchElementException(s"No Type Persistence matching with object structure array '${args.mkString("(", ", ", ")")}' has been found")
    }
}
