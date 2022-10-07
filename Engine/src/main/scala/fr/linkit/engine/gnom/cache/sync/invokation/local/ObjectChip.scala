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

package fr.linkit.engine.gnom.cache.sync.invokation.local

import fr.linkit.api.gnom.cache.sync._
import fr.linkit.api.gnom.cache.sync.contract.{MethodContract, StructureContract}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationHandlingMethod._
import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.cache.sync.invocation.{HiddenMethodInvocationException, MirroringObjectInvocationException}
import fr.linkit.api.gnom.network.{Engine, ExecutorEngine, Network}
import fr.linkit.api.internal.concurrency.workerExecution
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.internal.debug.{Debugger, MethodInvocationComputeStep, MethodInvocationExecutionStep}
import fr.linkit.engine.internal.util.ScalaUtils
import org.jetbrains.annotations.Nullable

import scala.annotation.switch
import scala.util.control.NonFatal

class ObjectChip[A <: AnyRef] private(contract: StructureContract[A],
                                      network : Network, chippedObject: ChippedObject[A]) extends Chip[A] {

    private val chipped   = chippedObject.connected
    private val isDistant = contract.mirroringInfo.isDefined
    private val isOrigin  = chippedObject.isOrigin

    override def updateObject(obj: A): Unit = {
        ScalaUtils.pasteAllFields(chipped, obj)
    }

    override def callMethod(methodID: Int, params: Array[Any], @Nullable caller: Engine)(onException: Throwable => Unit, @workerExecution onResult: Any => Unit): Unit = try {
        val callerID = if (caller == null) null else caller.identifier
        Debugger.push(MethodInvocationComputeStep(methodID, callerID, callerID == currentIdentifier))

        val methodContract = contract.findMethodContract[Any](methodID).getOrElse {
            throw new NoSuchElementException(s"Could not find method contract with identifier #$methodID for ${chippedObject.getClassDef}.")
        }
        val hideMsg        = methodContract.hideMessage
        if (hideMsg.isDefined)
            throw new HiddenMethodInvocationException(hideMsg.get)
        if (!isOrigin && isDistant)
            throw new MirroringObjectInvocationException(s"Attempted to call a method on a distant object representation. This object is mirroring ${chippedObject.reference} on engine ${chippedObject.ownerID}")
        val procrastinator = methodContract.procrastinator
        if (procrastinator != null) {
            AppLoggers.COInv.debug(s"Calling Method in another Procrastinator - '$methodID'")
            procrastinator.runLater {
                val result = callMethod(methodContract, params, caller)(onException)
                network.connection.runLater(onResult(result)) //return back to initial worker pool
            }
        } else {
            onResult(callMethod(methodContract, params, caller)(onException))
        }
    } finally Debugger.pop()

    @inline private def callMethod(contract: MethodContract[Any], params: Array[Any], @Nullable caller: Engine)(onException: Throwable => Unit): Any = {
        val invKind = contract.invocationHandlingMethod

        def call() = contract.choreographer.disinv {
            ExecutorEngine.setCurrentEngine(caller)
            val data   = new contract.InvocationExecution {
                override val obj      : ChippedObject[_] = ObjectChip.this.chippedObject
                override val arguments: Array[Any]       = params
            }
            val result = try {
                contract.executeMethodInvocation(caller, data)
            } catch {
                case NonFatal(e) => onException(e)
            }
            ExecutorEngine.setCurrentEngine(network.currentEngine) //return to the current engine.
            result
        }

        val choreographer = chippedObject.getChoreographer
        import choreographer._
        (invKind: @switch) match {
            case DisableSubInvocations => disinv(call())
            case EnableSubInvocations  => ensinv(call())
            case Inherit               => call()
        }
    }

    private def currentIdentifier = network.currentEngine.identifier

}

object ObjectChip {


    def apply[S <: AnyRef](contract: StructureContract[S], network: Network, chippedObject: ChippedObject[S]): ObjectChip[S] = {
        if (chippedObject == null)
            throw new NullPointerException("chipped object is null !")
        new ObjectChip[S](contract, network, chippedObject)
    }

}
