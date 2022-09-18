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

package fr.linkit.engine.gnom.cache.sync.contract.description

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.{FieldDescription, MethodDescription, SyncClassDef}
import fr.linkit.api.gnom.persistence.context.Deconstructible
import fr.linkit.api.gnom.persistence.context.Deconstructible.Persist
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription.isNotOverrideable
import fr.linkit.engine.gnom.cache.sync.generation.SyncClassRectifier.JavaKeywords

import java.lang.reflect.{Executable, Method, Modifier}
import scala.collection.mutable

class SyncObjectDescription[A <: AnyRef] @Persist() protected(private val clazz: SyncClassDef) extends AbstractSyncStructureDescription[A](clazz) with Deconstructible {
    
    private val overrideableDescs: Iterable[MethodDescription] = {
        listMethods().filterNot(md => notFilter(true, md.javaMethod))
    }
    
    def listOverrideableMethods(): Iterable[MethodDescription] = overrideableDescs
    
    def listMethods[L >: A](limit: Class[L]): Iterable[MethodDescription] = {
        listMethods().filter(m => limit.isAssignableFrom(m.javaMethod.getDeclaringClass))
    }
    
    def listFields[L >: A](limit: Class[L]): Iterable[FieldDescription] = {
        listFields().filter(f => limit.isAssignableFrom(f.javaField.getDeclaringClass))
    }
    
    override protected def applyNotFilter(e: Executable): Boolean = {
        notFilter(false, e)
    }
    
    private def notFilter(mustBeOverrideable: Boolean, e: Executable) = {
        if (!mustBeOverrideable) {
            import Modifier._
            val mods = e.getModifiers
            isPrivate(mods) || isStatic(mods) || !isPublic(mods) || isIllegal(e)
        }  else isNotOverrideable(e.getModifiers) || containsNonAccessibleElements(e) || isIllegal(e)
    }
    
    override def deconstruct(): Array[Any] = Array(clazz)
    
    private def isIllegal(e: Executable): Boolean = {
        val isNameJKeyword = JavaKeywords(e.getName)
        if (isNameJKeyword)
            AppLoggers.ConnObj.warn(s"Cannot handle method ${e} because its name is a java keyword.")
        isNameJKeyword
    }
    
    private def containsNonAccessibleElements(e: Executable): Boolean = {
        def isNotAccessible(clazz: Class[_], tpe: String): Boolean = {
            if (clazz.isPrimitive)
                return false
            val mods = clazz.getModifiers
            import Modifier._
            val notAccessible = isPrivate(mods) || !(isProtected(mods) || isPublic(mods))
            if (notAccessible)
                AppLoggers.ConnObj.warn(s"Cannot handle method ${e} because $tpe '${clazz.getName}' is not accessible for the generated Sync implementation class of '${this.clazz}'")
            notAccessible
        }
        
        e match {
            case method: Method =>
                isNotAccessible(method.getReturnType, "return type") ||
                        method.getParameterTypes.exists(isNotAccessible(_, "parameter type"))
            case _              => false
        }
    }

    override def equals(obj: Any): Boolean = obj match {
        case desc: SyncObjectDescription[_] =>
            (desc eq this) || (desc.clazz == clazz && desc.listMethods() == listMethods())
        case _ => false
    }
}

object SyncObjectDescription {
    

    private val cache = mutable.HashMap.empty[Int, SyncObjectDescription[_]]
    
    def apply[A <: AnyRef](specs: SyncClassDef): SyncObjectDescription[A] = cache.getOrElseUpdate(specs.id, {
        if (specs.isAssignableFromThis(classOf[SynchronizedObject[_]]))
            throw new IllegalArgumentException(s"Provided class definition contains classes that extends ${classOf[SynchronizedObject[_]]} ($specs)")
        new SyncObjectDescription[A](specs)
    }).asInstanceOf[SyncObjectDescription[A]]
    
    //implicit def fromTag[A <: AnyRef : ClassTag]: SyncObjectDescription[A] = apply[A](classTag[A].runtimeClass)
    
    def isNotOverrideable(mods: Int): Boolean = {
        import Modifier._
        isPrivate(mods) || isStatic(mods) || isFinal(mods) || !isPublic(mods)
    }
}