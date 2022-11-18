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

import fr.linkit.api.gnom.persistence.context.{TypePersistor, TypeProfile, TypeProfileStore}
import fr.linkit.engine.gnom.persistence.PersistenceException

class DefaultTypeProfile[T <: AnyRef](override val typeClass: Class[_],
                                      store: TypeProfileStore,
                                      private[config] val persists: Array[TypePersistor[T]]) extends TypeProfile[T] {
    
    private lazy val declaredParent: TypeProfile[_ >: T] = {
        val superClass = typeClass.getSuperclass
        if (superClass == null) null else store.getProfile[T](superClass)
    }
    
    override def getPersistences: Array[TypePersistor[T]] = persists
    
    override def selectPersistor(t: T, selectionChoice: Int): TypePersistor[T] = {
        if (selectionChoice > persists.length)
            throw new PersistenceException(s"Could not select persistor n° $selectionChoice")
        persists(selectionChoice)
    }
    
    override def selectPersistor(args: Array[Any]): TypePersistor[T] = {
        var i   = 0
        val len = persists.length
        while (i < len) {
            val persist = persists(i)
            if (persist.structure.isAssignable(args))
                return persist
            i += 1
        }
        val result = if (declaredParent ne null) declaredParent.selectPersistor(args) else null
        if (result == null)
            throw new NoSuchElementException(errorMsg(args, typeClass))
        result
    }

    private def errorMsg(args: Array[Any], baseClass: Class[_]): String = {
        s"No type persistence matching with object structure array '${args.mkString("(", ", ", ")")}' has been found, for type profile of class '$baseClass'"
    }

}