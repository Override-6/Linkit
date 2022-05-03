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

package fr.linkit.api.gnom.cache.sync.contract.description

import fr.linkit.api.gnom.cache.sync.InvalidSyncClassDefinitionException
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef.check

import java.util

class SyncClassDef(val mainClass: Class[_]) {

    check(mainClass)

    val id: Int = mainClass.getName.hashCode

    override def toString: String = s"SyncClassDef(superClass: $mainClass)"

    def isAssignableFromThis(clazz: Class[_]): Boolean = clazz.isAssignableFrom(mainClass)
}

class SyncClassDefMultiple(superClass: Class[_], val interfaces: Array[Class[_]] = Array()) extends SyncClassDef(superClass) {

    interfaces.foreach(check)

    override val id: Int = util.Arrays.hashCode(Array[AnyRef](superClass.getName) ++ interfaces.map(_.getName)).abs

    override def toString: String = s"SyncClassDefMultiple(superClass: $superClass, interfaces: ${interfaces.mkString("Array(", ", ", ")")})"

    override def isAssignableFromThis(clazz: Class[_]): Boolean = super.isAssignableFromThis(clazz) || interfaces.exists(clazz.isAssignableFrom)

}

object SyncClassDef {

    private[description] def check(clazz: Class[_]): Unit = {
        if (clazz.isArray)
            throw new InvalidSyncClassDefinitionException(s"Provided class definition array class. ($clazz)")

        import java.lang.reflect.Modifier._
        val mods = clazz.getModifiers
        if (isFinal(mods) || !isPublic(mods))
            throw new InvalidSyncClassDefinitionException(s"Provided class definition is not overrideable. ($clazz)")
    }

    def apply(superClass: Class[_]): SyncClassDef = {
        new SyncClassDef(superClass)
    }
}

object SyncClassDefMultiple {

    def apply(superClass: Class[_], interfaces: Array[Class[_]]): SyncClassDefMultiple = new SyncClassDefMultiple(superClass, interfaces)
}
