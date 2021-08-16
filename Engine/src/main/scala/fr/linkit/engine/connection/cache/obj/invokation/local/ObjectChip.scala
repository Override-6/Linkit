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

import fr.linkit.api.connection.cache.obj._
import fr.linkit.api.connection.cache.obj.behavior.SynchronizedObjectBehavior
import fr.linkit.api.connection.cache.obj.invokation.local.Chip
import fr.linkit.api.local.concurrency.{Procrastinator, WorkerPools}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.obj.invokation.local.ObjectChip.NoResult
import fr.linkit.engine.local.utils.ScalaUtils

import java.lang.reflect.{Method, Modifier}
import java.util.concurrent.locks.LockSupport

class ObjectChip[S] private(behavior: SynchronizedObjectBehavior[S],
                            wrapper: SynchronizedObject[S]) extends Chip[S] {

    override def updateObject(obj: S): Unit = {
        ScalaUtils.pasteAllFields(wrapper, obj)
    }

    override def callMethod(methodID: Int, params: Array[Any]): Any = {
        val methodBehaviorOpt = behavior.getMethodBehavior(methodID)
        if (methodBehaviorOpt.forall(_.isHidden)) {
            throw new SyncObjectException(s"Attempted to invoke ${methodBehaviorOpt.fold("unknown")(_ => "hidden")} method '${
                methodBehaviorOpt.map(_.desc.symbol.name.toString).getOrElse(s"(unknown method id '$methodID')")
            }(${params.mkString(", ")}) in class ${methodBehaviorOpt.map(_.desc.symbol).getOrElse("Unknown")}'")
        }
        val methodBehavior = methodBehaviorOpt.get
        val name           = methodBehavior.desc.javaMethod.getName
        val procrastinator = methodBehavior.procrastinator
        val method         = methodBehavior.desc.javaMethod
        AppLogger.debug(s"RMI - Calling method $methodID $name(${params.mkString(", ")})")
        if (procrastinator != null) {
            callMethodProcrastinator(procrastinator, method, params)
        } else {
            callMethod(method, params)
        }
    }

    @inline private def callMethod(method: Method, params: Array[Any]): Any = {
        wrapper.getChoreographer.forceLocalInvocation {
            method.invoke(wrapper, params: _*)
        }
    }

    @inline private def callMethodProcrastinator(procrastinator: Procrastinator, method: Method, params: Array[Any]): Any = {
        @volatile var result: Any = NoResult
        val worker      = WorkerPools.currentWorker
        val task        = WorkerPools.currentTask
        val pool        = worker.pool
        procrastinator.runLater {
            result = callMethod(method, params)
            task.wakeup()
        }
        if (result == NoResult)
            pool.pauseCurrentTask()
        result
    }
}

object ObjectChip {

    def apply[S](behavior: SynchronizedObjectBehavior[S], wrapper: SynchronizedObject[S]): ObjectChip[S] = {
        if (wrapper == null)
            throw new NullPointerException("puppet is null !")
        val clazz = wrapper.getClass

        if (Modifier.isFinal(clazz.getModifiers))
            throw new IllegalObjectWrapperException("Puppet can't be final.")

        new ObjectChip[S](behavior, wrapper)
    }

    object NoResult
}
