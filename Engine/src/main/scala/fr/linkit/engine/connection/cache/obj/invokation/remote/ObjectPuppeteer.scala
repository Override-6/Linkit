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

package fr.linkit.engine.connection.cache.obj.invokation.remote

import fr.linkit.api.connection.cache.obj._
import fr.linkit.api.connection.cache.obj.behavior.{RMIRulesAgreement, SynchronizedObjectBehavior}
import fr.linkit.api.connection.cache.obj.description.SyncNodeInfo
import fr.linkit.api.connection.cache.obj.invokation.remote.{Puppeteer, SynchronizedMethodInvocation}
import fr.linkit.api.connection.packet.channel.request.RequestPacketChannel
import fr.linkit.api.local.concurrency.ProcrastinatorControl
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.obj.ThrowableWrapper
import fr.linkit.engine.connection.packet.fundamental.RefPacket

class ObjectPuppeteer[S <: AnyRef](channel: RequestPacketChannel,
                                   procrastinator: ProcrastinatorControl,
                                   override val center: SynchronizedObjectCenter[_],
                                   override val nodeInfo: SyncNodeInfo,
                                   val wrapperBehavior: SynchronizedObjectBehavior[S]) extends Puppeteer[S] {

    private      val traffic                                         = channel.traffic
    override     val currentIdentifier: String                       = traffic.currentIdentifier
    private lazy val tree                                            = center.treeCenter.findTree(nodeInfo.nodePath.head).get
    private      val writer                                          = traffic.newWriter(channel.identifier)
    private var puppetWrapper         : S with SynchronizedObject[S] = _

    override def isCurrentEngineOwner: Boolean = ownerID == currentIdentifier

    override def getSynchronizedObject: S with SynchronizedObject[S] = puppetWrapper

    override def sendInvokeAndWaitResult[R](agreement: RMIRulesAgreement, invocation: SynchronizedMethodInvocation[R]): R = {
        if (!agreement.mayPerformRemoteInvocation)
            throw new IllegalAccessException("agreement may not perform remote invocation")

        val bhv          = invocation.methodBehavior
        val methodId     = bhv.desc.methodId
        val treeViewPath = nodeInfo.nodePath
        val args         = invocation.methodArguments

        AppLogger.debug(s"Remotely invoking method ${bhv.desc.symbol.name}")
        val scope = new AgreementScope(writer, agreement)
        center.drainAllDefaultAttributes(scope)
        val result = channel.makeRequest(scope)
            .addPacket(InvocationPacket(treeViewPath, methodId, args, agreement.getDesiredEngineReturn))
            .submit()
            .nextResponse
            .nextPacket[RefPacket[R]].value
        result match {
            //FIXME ambiguity with broadcast method invocation.
            case ThrowableWrapper(e) => throw new RemoteInvocationFailedException(s"Invocation of method $methodId with arguments '${args.mkString(", ")}' failed.", e)
            case result              => result
        }
    }

    override def sendInvoke(agreement: RMIRulesAgreement, invocation: SynchronizedMethodInvocation[_]): Unit = {
        if (!agreement.mayPerformRemoteInvocation)
            throw new IllegalAccessException("agreement may not perform remote invocation")

        procrastinator.runLater {
            val bhv      = invocation.methodBehavior
            val args     = invocation.methodArguments
            val methodId = bhv.desc.methodId

            val scope = new AgreementScope(writer, agreement)
            center.drainAllDefaultAttributes(scope)
            AppLogger.debug(s"Remotely invoking method asynchronously ${bhv.desc.symbol.name}(${args.mkString(",")})")
            channel.makeRequest(scope)
                .addPacket(InvocationPacket(nodeInfo.nodePath, methodId, args, null))
                .submit()
                .detach()
        }
    }

    override def init(wrapper: S with SynchronizedObject[S]): Unit = {
        if (this.puppetWrapper != null) {
            throw new IllegalStateException("This Puppeteer already controls a puppet instance !")
        }
        this.puppetWrapper = wrapper
    }

    override def synchronizedObj(obj: AnyRef, id: Int): AnyRef with SynchronizedObject[AnyRef] = {
        val currentPath = nodeInfo.nodePath
        tree.insertObject(currentPath, id, obj, currentIdentifier).synchronizedObject
    }

}
