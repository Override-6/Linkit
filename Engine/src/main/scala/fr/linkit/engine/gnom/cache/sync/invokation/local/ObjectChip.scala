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
import fr.linkit.api.gnom.cache.sync.contract.{MethodContract, SynchronizedStructureContract}
import fr.linkit.api.gnom.cache.sync.contractv2.modification.ValueModifierKind
import fr.linkit.api.gnom.cache.sync.invocation.local.{Chip, LocalMethodInvocation}
import fr.linkit.api.gnom.network.{Engine, ExecutorEngine, Network}
import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.gnom.cache.sync.BadRMIRequestException
import fr.linkit.engine.gnom.cache.sync.invokation.AbstractMethodInvocation
import fr.linkit.engine.gnom.cache.sync.invokation.local.ObjectChip.NoResult
import fr.linkit.engine.internal.utils.ScalaUtils

import java.lang.reflect.Modifier

class ObjectChip[S <: AnyRef] private(contract: SynchronizedStructureContract[S],
                                      syncObject: SynchronizedObject[S],
                                      network: Network) extends Chip[S] {

    private val behavior = contract.behavior

    override def updateObject(obj: S): Unit = {
        ScalaUtils.pasteAllFields(syncObject, obj)
    }

    override def callMethod(methodID: Int, params: Array[Any], origin: Engine): Any = {
        val methodContractOpt = contract.getMethodContract(methodID)
        if (methodContractOpt.forall(_.behavior.isHidden)) {
            throw new BadRMIRequestException(s"Attempted to invoke ${methodContractOpt.fold("unknown")(_ => "hidden")} method '${
                methodContractOpt.map(_.description.javaMethod.getName).getOrElse(s"(unknown method id '$methodID')")
            }(${params.mkString(", ")}) in ${methodContractOpt.map(_.description.classDesc.clazz).getOrElse("Unknown class")}'")
        }
        val methodContract = methodContractOpt.get
        val procrastinator = methodContract.procrastinator
        AppLogger.debug {
            val name = methodContract.description.javaMethod.getName
            s"RMI - Calling method $methodID $name(${params.mkString(", ")})"
        }
        if (procrastinator != null) {
            callMethodProcrastinator(methodContract, params, origin)
        } else {
            callMethod(methodContract, params, origin)
        }
    }

    @inline private def callMethod(contract: MethodContract, params: Array[Any], origin: Engine): Any = {
        syncObject.getChoreographer.forceLocalInvocation {
            modifyParameters(contract, origin, params)
            ExecutorEngine.setCurrentEngine(origin)
            val result = contract.description.javaMethod.invoke(syncObject, params: _*)
            ExecutorEngine.setCurrentEngine(network.connectionEngine) //return to the current engine.
            result
        }
    }

    @inline
    private def modifyParameters(contract: MethodContract, engine: Engine, params: Array[Any]): Unit = {
        val paramsContracts = contract.parameterContracts
        if (paramsContracts.isEmpty)
            return
        for (i <- params.indices) {
            val contract = paramsContracts(i)

            val modifier = contract.modifier.orNull
            var result   = params(i)
            if (modifier != null) {
                result = modifier.fromRemote(result,  engine)
                modifier.fromRemoteEvent(result,  engine)
            }
            params(i) = result
        }
    }

    @inline private def callMethodProcrastinator(contract: MethodContract, params: Array[Any], origin: Engine): Any = {
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

    def apply[S <: AnyRef](contract: SynchronizedStructureContract[S], network: Network, wrapper: SynchronizedObject[S]): ObjectChip[S] = {
        if (wrapper == null)
            throw new NullPointerException("puppet is null !")
        val clazz = wrapper.getClass

        if (Modifier.isFinal(clazz.getModifiers))
            throw new CanNotSynchronizeException("Puppet can't be final.")

        new ObjectChip[S](contract, wrapper, network)
    }

    object NoResult

}
