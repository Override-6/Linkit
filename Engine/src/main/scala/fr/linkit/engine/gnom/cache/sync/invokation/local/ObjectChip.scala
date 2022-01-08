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

package fr.linkit.engine.gnom.cache.sync.invokation.local

import fr.linkit.api.gnom.cache.sync._
import fr.linkit.api.gnom.cache.sync.contractv2.{MethodContract, StructureContract}
import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.cache.sync.tree.ObjectSyncNode
import fr.linkit.api.gnom.network.{Engine, ExecutorEngine, Network}
import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.engine.gnom.cache.sync.invokation.local.ObjectChip.NoResult
import fr.linkit.engine.internal.utils.ScalaUtils

import java.lang.reflect.Modifier

class ObjectChip[S <: AnyRef] private(contract: StructureContract[S],
                                      syncObject: SynchronizedObject[S],
                                      network: Network) extends Chip[S] {

    override def updateObject(obj: S): Unit = {
        ScalaUtils.pasteAllFields(syncObject, obj)
    }

    override def callMethod(methodID: Int, params: Array[Any], origin: Engine): Any = {
        val methodContract = contract.getMethodContract[Any](methodID)
        val procrastinator = methodContract.procrastinator
        if (procrastinator != null) {
            callMethodProcrastinator(methodContract, params, origin)
        } else {
            callMethod(methodContract, params, origin)
        }
    }

    @inline private def callMethod(contract: MethodContract[Any], params: Array[Any], origin: Engine): Any = {
        syncObject.getChoreographer.forceLocalInvocation {
            ExecutorEngine.setCurrentEngine(origin)
            val data   = new contract.InvocationExecution {
                override val operatingNode        : ObjectSyncNode[_] = syncObject.getNode
                override val arguments            : Array[Any]        = params
            }
            val result = contract.executeMethodInvocation(origin, data)
            ExecutorEngine.setCurrentEngine(network.connectionEngine) //return to the current engine.
            result
        }
    }

    @inline private def callMethodProcrastinator(contract: MethodContract[Any], params: Array[Any], origin: Engine): Any = {
        @volatile var result: Any = NoResult
        val worker                = WorkerPools.currentWorker
        val task                  = WorkerPools.currentTask.get
        val pool                  = worker.pool
        contract.procrastinator.runLater {
            result = callMethod(contract, params, origin)
            task.wakeup()
        }
        if (result == NoResult)
            pool.pauseCurrentTask()
        result
    }
}

object ObjectChip {

    def apply[S <: AnyRef](contract: StructureContract[S], network: Network, wrapper: SynchronizedObject[S]): ObjectChip[S] = {
        if (wrapper == null)
            throw new NullPointerException("puppet is null !")
        val clazz = wrapper.getClass

        if (Modifier.isFinal(clazz.getModifiers))
            throw new CanNotSynchronizeException("Puppet can't be final.")

        new ObjectChip[S](contract, wrapper, network)
    }

    object NoResult

}
