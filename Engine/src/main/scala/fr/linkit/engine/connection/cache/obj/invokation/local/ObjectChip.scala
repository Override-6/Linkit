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
import fr.linkit.api.connection.cache.obj.behavior.SynchronizedObjectBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.MethodBehavior
import fr.linkit.api.connection.cache.obj.invokation.local.{Chip, LocalMethodInvocation}
import fr.linkit.api.connection.network.Network
import fr.linkit.api.local.concurrency.WorkerPools
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.obj.invokation.AbstractMethodInvocation
import fr.linkit.engine.connection.cache.obj.invokation.local.ObjectChip.NoResult
import fr.linkit.engine.local.utils.ScalaUtils

class ObjectChip[S <: AnyRef] private(behavior: SynchronizedObjectBehavior[S],
                                      syncObject: SynchronizedObject[S],
                                      network: Network) extends Chip[S] {

    override def updateObject(obj: S): Unit = {
        ScalaUtils.pasteAllFields(syncObject, obj)
    }

    override def callMethod(methodID: Int, params: Array[Any], origin: String): Any = {
        val methodBehaviorOpt = behavior.getMethodBehavior(methodID)
        if (methodBehaviorOpt.forall(_.isHidden)) {
            throw new SyncObjectException(s"Attempted to invoke ${methodBehaviorOpt.fold("unknown")(_ => "hidden")} method '${
                methodBehaviorOpt.map(_.desc.symbol.name.toString).getOrElse(s"(unknown method id '$methodID')")
            }(${params.mkString(", ")}) in class ${methodBehaviorOpt.map(_.desc.symbol).getOrElse("Unknown")}'")
        }
        val methodBehavior = methodBehaviorOpt.get
        val name           = methodBehavior.desc.javaMethod.getName
        val procrastinator = methodBehavior.procrastinator
        AppLogger.debug(s"RMI - Calling method $methodID $name(${params.mkString(", ")})")
        if (procrastinator != null) {
            callMethodProcrastinator(methodBehavior, params, origin)
        } else {
            callMethod(methodBehavior, params, origin)
        }
    }

    @inline private def callMethod(behavior: MethodBehavior, params: Array[Any], origin: String): Any = {
        syncObject.getChoreographer.forceLocalInvocation {
            //TODO cache action in the behavior
            lazy val invocation = new AbstractMethodInvocation[Any](behavior, syncObject) with LocalMethodInvocation[Any] {
                /**
                 * The final argument array for the method invocation.
                 * */
                override val methodArguments: Array[Any] = params
            }
            lazy val engine     = network.findEngine(origin).get
            val paramsBehaviors = behavior.parameterBehaviors
            for (i <- params.indices) {
                val modifier = paramsBehaviors(i).modifier
                if (modifier != null)
                    params(i) = modifier.forLocalComingFromRemote(params(i), invocation, engine)
            }
            behavior.desc.javaMethod.invoke(syncObject, params: _*)
        }
    }

    @inline private def callMethodProcrastinator(behavior: MethodBehavior, params: Array[Any], origin: String): Any = {
        @volatile var result: Any = NoResult
        val worker                = WorkerPools.currentWorker
        val task                  = WorkerPools.currentTask
        val pool                  = worker.pool
        behavior.procrastinator.runLater {
            result = callMethod(behavior, params, origin)
            task.wakeup()
        }
        if (result == NoResult)
            pool.pauseCurrentTask()
        result
    }
}

object ObjectChip {

    def apply[S <: AnyRef](behavior: SynchronizedObjectBehavior[S], network: Network, wrapper: SynchronizedObject[S]): ObjectChip[S] = {
        if (wrapper == null)
            throw new NullPointerException("puppet is null !")
        val clazz = wrapper.getClass

        if (Modifier.isFinal(clazz.getModifiers))
            throw new IllegalSynchronizationException("Puppet can't be final.")

        new ObjectChip[S](behavior, wrapper, network)
    }

    object NoResult

}
