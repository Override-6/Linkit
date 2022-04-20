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
import fr.linkit.api.gnom.cache.sync.contract.{MethodContract, StructureContract}
import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.cache.sync.invocation.{HiddenMethodInvocationException, InvocationChoreographer, MirroringObjectInvocationException}
import fr.linkit.api.gnom.network.{Engine, ExecutorEngine, Network}
import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.engine.gnom.cache.sync.invokation.local.ObjectChip.NoResult
import fr.linkit.engine.internal.utils.ScalaUtils

import java.lang.reflect.Modifier

class ObjectChip[S <: AnyRef] private(contract: StructureContract[S],
                                      network: Network)
                                     (chipped: S, isOrigin: Boolean, sourceClass: Class[_], choreographer: InvocationChoreographer)
        extends Chip[S] {

    private final val isDistant = contract.remoteObjectInfo.isDefined

    override def updateObject(obj: S): Unit = {
        ScalaUtils.pasteAllFields(chipped, obj)
    }

    override def callMethod(methodID: Int, params: Array[Any], caller: Engine): Any = {
        val methodContract = contract.findMethodContract[Any](methodID).getOrElse {
            throw new NoSuchElementException(s"Could not find method contract with identifier #$methodID for ${sourceClass}.")
        }
        val hideMsg        = methodContract.hideMessage
        if (hideMsg.isDefined)
            throw new HiddenMethodInvocationException(hideMsg.get)
        if (!isOrigin && isDistant)
            throw new MirroringObjectInvocationException(s"Attempted to call a method on a distant object representation. This object is mirroring ${chipped.reference} on engine ${chipped.ownerID}")
        val procrastinator = methodContract.procrastinator
        if (procrastinator != null) {
            callMethodProcrastinator(methodContract, params, caller)
        } else {
            callMethod(methodContract, params, caller)
        }
    }

    @inline private def callMethod(contract: MethodContract[Any], params: Array[Any], caller: Engine): Any = {
        choreographer.disableInvocations {
            ExecutorEngine.setCurrentEngine(caller)
            val data   = new contract.InvocationExecution {
                override val syncObject: SynchronizedObject[_] = ObjectChip.this.chipped
                override val arguments : Array[Any]            = params
            }
            val result = contract.executeMethodInvocation(caller, data)
            ExecutorEngine.setCurrentEngine(network.connectionEngine) //return to the current engine.
            result
        }
    }

    @inline private def callMethodProcrastinator(contract: MethodContract[Any], params: Array[Any], caller: Engine): Any = {
        @volatile var result: Any = NoResult
        val worker                = WorkerPools.currentWorker
        val task                  = WorkerPools.currentTask.get
        val pool                  = worker.pool
        contract.procrastinator.runLater {
            result = callMethod(contract, params, caller)
            task.wakeup()
        }
        if (result == NoResult)
            pool.pauseCurrentTask()
        result
    }
}

object ObjectChip {

    object NoResult

    def apply[S <: AnyRef](contract: StructureContract[S], network: Network, chipped: S): ObjectChip[S] = {
        if (chipped == null)
            throw new NullPointerException("sync object is null !")
        new ObjectChip[S](contract, chipped, network)
    }

}
