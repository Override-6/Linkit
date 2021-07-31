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

package fr.linkit.engine.connection.cache.obj.invokation.local

import java.lang.reflect.Modifier

import fr.linkit.api.connection.cache.obj._
import fr.linkit.api.connection.cache.obj.description.WrapperBehavior
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.local.utils.ScalaUtils

class ObjectChip[S] private(owner: String,
                            behavior: WrapperBehavior[S],
                            wrapper: PuppetWrapper[S]) extends Chip[S] {

    override def updateObject(obj: S): Unit = {
        ScalaUtils.pasteAllFields(wrapper, obj)
    }

    override def callMethod(methodID: Int, params: Array[Any]): Any = {
        val methodBehaviorOpt = behavior.getMethodBehavior(methodID)
        if (methodBehaviorOpt.forall(_.isHidden)) {
            throw new PuppetException(s"Attempted to invoke ${methodBehaviorOpt.fold("unknown")(_ => "hidden")} method '${
                methodBehaviorOpt.map(_.desc.symbol.name.toString).getOrElse(s"(unknown method id '$methodID')")
            }(${params.mkString(", ")}) in class ${methodBehaviorOpt.map(_.desc.symbol).getOrElse("Unknown")}'")
        }
        val methodBehavior = methodBehaviorOpt.get
        val name           = methodBehavior.desc.javaMethod.getName
        wrapper.getChoreographer.forceLocalInvocation {
            AppLogger.debug(s"RMI - Calling method $methodID $name(${params.mkString(", ")})")
            methodBehaviorOpt.get
                .desc
                .javaMethod
                .invoke(wrapper, params: _*)
        }
    }

}

object ObjectChip {

    def apply[S](owner: String, behavior: WrapperBehavior[S], wrapper: PuppetWrapper[S]): ObjectChip[S] = {
        if (wrapper == null)
            throw new NullPointerException("puppet is null !")
        val clazz = wrapper.getClass

        if (Modifier.isFinal(clazz.getModifiers))
            throw new IllegalObjectWrapperException("Puppet can't be final.")

        new ObjectChip[S](owner, behavior, wrapper)
    }

}
