/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.persistence.config.profile

import fr.linkit.api.gnom.persistence.context.{TypePersistence, TypeProfile, TypeProfileStore}

class DefaultTypeProfile[T <: AnyRef](override val typeClass: Class[_],
                                      store: TypeProfileStore,
                                      private[config] val persists: Array[TypePersistence[T]]) extends TypeProfile[T] {
    
    private lazy val declaredParent: TypeProfile[_ >: T] = {
        val superClass = typeClass.getSuperclass
        if (superClass == null) null else store.getProfile[T](superClass)
    }
    
    override def getPersistences: Array[TypePersistence[T]] = persists
    
    override def getPersistence(t: T): TypePersistence[T] = {
        return persists.head
        /*//TODO Choose between other compatible persistence (if an error occurred etc...)
        if (declaredParent ne null)
            declaredParent.getPersistence(t)
        else
            throw new NoSuchElementException(s"Could not find type persistence matching object ${t} (of class ${t.getClass.getName}.")*/
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
        val result = if (declaredParent ne null) declaredParent.getPersistence(args) else null
        if (result == null)
            throw new NoSuchElementException(errorMsg(args, typeClass))
        result
    }

    private def errorMsg(args: Array[Any], baseClass: Class[_]): String = {
        s"No type persistence matching with object structure array '${args.mkString("(", ", ", ")")}' has been found, for type profile of class '$baseClass'"
    }

}