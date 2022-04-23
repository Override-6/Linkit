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
import fr.linkit.api.gnom.cache.sync.invocation.{HiddenMethodInvocationException, MirroringObjectInvocationException}
import fr.linkit.api.gnom.network.{Engine, ExecutorEngine, Network}
import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.engine.gnom.cache.sync.invokation.local.ObjectChip.NoResult
import fr.linkit.engine.internal.utils.ScalaUtils

class ObjectChip[A <: AnyRef] private(contract: StructureContract[A],
                                      network: Network, chippedObject: ChippedObject[A])
        extends Chip[A] {

    private val chipped       = chippedObject.connected
    private val sourceClass   = chippedObject.getConnectedObjectClass
    private val isDistant     = contract.remoteObjectInfo.isDefined
    private val isOrigin      = chippedObject.isOrigin

    override def updateObject(obj: A): Unit = {
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
            throw new MirroringObjectInvocationException(s"Attempted to call a method on a distant object representation. This object is mirroring ${chippedObject.reference} on engine ${chippedObject.ownerID}")
        val procrastinator = methodContract.procrastinator
        if (procrastinator != null) {
            callMethodProcrastinator(methodContract, params, caller)
        } else {
            callMethod(methodContract, params, caller)
        }
    }

    @inline private def callMethod(contract: MethodContract[Any], params: Array[Any], caller: Engine): Any = {
        chippedObject.getChoreographer.disableInvocations {
            ExecutorEngine.setCurrentEngine(caller)
            val data   = new contract.InvocationExecution {
                override val obj: ChippedObject[_] = ObjectChip.this.chippedObject
                override val arguments : Array[Any]       = params
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

    def apply[S <: AnyRef](contract: StructureContract[S], network: Network, chippedObject: ChippedObject[S]): ObjectChip[S] = {
        if (chippedObject == null)
            throw new NullPointerException("chipped object is null !")
        new ObjectChip[S](contract, network, chippedObject)
    }

}
