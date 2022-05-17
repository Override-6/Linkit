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
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDefUnique.check

import java.util

sealed trait SyncClassDef {

    val mainClass: Class[_]
    val id       : Int

    def isAssignableFromThis(clazz: Class[_]): Boolean

    def ensureOverrideable(): Unit
}

final class SyncClassDefUnique(val mainClass: Class[_]) extends SyncClassDef {

    val id: Int = mainClass.getName.hashCode.abs

    override def toString: String = s"class def ${mainClass.getName}"

    def isAssignableFromThis(clazz: Class[_]): Boolean = clazz.isAssignableFrom(mainClass)

    override def ensureOverrideable(): Unit = check(mainClass)
}

final class SyncClassDefMultiple(val mainClass: Class[_], val interfaces: Array[Class[_]] = Array()) extends SyncClassDef {

    interfaces.foreach(cl => if (!cl.isInterface) throw new IllegalArgumentException(s"$cl is not an interface."))

    override val id: Int = util.Arrays.hashCode(Array[AnyRef](mainClass.getName) ++ interfaces.map(_.getName)).abs

    override def toString: String = s"class def ${mainClass.getName} with interfaces ${interfaces.map(_.getName).mkString(", ")}"

    override def isAssignableFromThis(clazz: Class[_]): Boolean = clazz.isAssignableFrom(mainClass) || interfaces.exists(clazz.isAssignableFrom)

    override def ensureOverrideable(): Unit = {
        check(mainClass)
        interfaces.foreach(check)
    }

}

object SyncClassDefUnique {

    private[description] def check(clazz: Class[_]): Unit = {
        if (clazz.isArray)
            throw new InvalidSyncClassDefinitionException(s"Provided class definition array class. ($clazz)")

        import java.lang.reflect.Modifier._
        val mods = clazz.getModifiers
        if (isFinal(mods) || !isPublic(mods))
            throw new InvalidSyncClassDefinitionException(s"Provided class definition is not overrideable. ($clazz)")
    }

    def apply(superClass: Class[_]): SyncClassDefUnique = {
        new SyncClassDefUnique(superClass)
    }
}

object SyncClassDefMultiple {

    def apply(superClass: Class[_], interfaces: Array[Class[_]]): SyncClassDefMultiple = new SyncClassDefMultiple(superClass, interfaces)
}
