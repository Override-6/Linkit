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
import fr.linkit.api.connection.cache.obj.behavior.{RMIRulesAgreement, WrapperBehavior}
import fr.linkit.api.connection.cache.obj.description.WrapperNodeInfo
import fr.linkit.api.connection.cache.obj.invokation.WrapperMethodInvocation
import fr.linkit.api.local.concurrency.ProcrastinatorControl
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.obj.ThrowableWrapper
import fr.linkit.engine.connection.cache.obj.invokation.SimpleRMIRulesAgreement
import fr.linkit.engine.connection.packet.fundamental.RefPacket
import fr.linkit.engine.connection.packet.traffic.channel.request.RequestPacketChannel

class InstancePuppeteer[S <: AnyRef](channel: RequestPacketChannel,
                                     procrastinator: ProcrastinatorControl,
                                     override val center: SynchronizedObjectCenter[_],
                                     override val puppeteerInfo: WrapperNodeInfo,
                                     val wrapperBehavior: WrapperBehavior[S]) extends Puppeteer[S] {

    private      val traffic                                    = channel.traffic
    override     val currentIdentifier: String                  = traffic.currentIdentifier
    private lazy val tree                                       = center.treeCenter.findTree(puppeteerInfo.nodePath.head).get
    override     val ownerID          : String                  = puppeteerInfo.owner
    private      val writer                                     = traffic.newWriter(channel.identifier)
    private var puppetWrapper         : S with PuppetWrapper[S] = _

    override def isCurrentEngineOwner: Boolean = ownerID == currentIdentifier

    override def getPuppetWrapper: S with PuppetWrapper[S] = puppetWrapper

    override def sendInvokeAndWaitResult[R](agreement: RMIRulesAgreement, invocation: WrapperMethodInvocation[R]): R = {
        if (!agreement.mayPerformRemoteInvocation)
            throw new IllegalAccessException("agreement may not perform remote invocation")

        val bhv          = invocation.methodBehavior
        val methodId     = bhv.desc.methodId
        val treeViewPath = puppeteerInfo.nodePath
        val args         = invocation.methodArguments

        AppLogger.debug(s"Remotely invoking method ${bhv.desc.symbol.name}")
        val scope     = new AgreementScope(writer, agreement)
        center.drainAllDefaultAttributes(scope)
        val result    = channel.makeRequest(scope)
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

    override def sendInvoke(agreement: RMIRulesAgreement, invocation: WrapperMethodInvocation[_]): Unit = {
        if (!agreement.mayPerformRemoteInvocation)
            throw new IllegalAccessException("agreement may not perform remote invocation")

        procrastinator.runLater {
            val bhv      = invocation.methodBehavior
            val args     = invocation.methodArguments
            val methodId = bhv.desc.methodId

            val scope     = new AgreementScope(writer, agreement)
            center.drainAllDefaultAttributes(scope)
            AppLogger.debug(s"Remotely invoking method asynchronously ${bhv.desc.symbol.name}(${args.mkString(",")})")
            channel.makeRequest(scope)
                    .addPacket(InvocationPacket(puppeteerInfo.nodePath, methodId, args, null))
                    .submit()
                    .detach()
        }
    }

    override def init(wrapper: S with PuppetWrapper[S]): Unit = {
        if (this.puppetWrapper != null) {
            throw new IllegalStateException("This Puppeteer already controls a puppet instance !")
        }
        this.puppetWrapper = wrapper
    }

    override def synchronizedObj(obj: AnyRef, id: Int): AnyRef with PuppetWrapper[AnyRef] = {
        val currentPath = puppeteerInfo.nodePath
        tree.insertObject(currentPath, id, obj, currentIdentifier).synchronizedObject
    }

}
