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

package fr.linkit.engine.connection.network.cache.repo

import fr.linkit.api.connection.network.cache.repo.generation.PuppeteerDescription
import fr.linkit.api.connection.network.cache.repo.{PuppetDescription, PuppetWrapper, Puppeteer, RemoteInvocationFailedException}
import fr.linkit.api.connection.packet.PacketAttributesPresence
import fr.linkit.api.connection.packet.channel.ChannelScope
import fr.linkit.api.connection.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.packet.fundamental.RefPacket
import fr.linkit.engine.connection.packet.fundamental.RefPacket.ObjectPacket
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.RequestPacketChannel

class SimplePuppeteer[S <: Serializable](channel: RequestPacketChannel,
                                         presence: PacketAttributesPresence,
                                         override val puppeteerDescription: PuppeteerDescription,
                                         val puppetDescription: PuppetDescription[S]) extends Puppeteer[S] {

    private val ownerScope = prepareScope(ChannelScopes.retains(puppeteerDescription.owner))
    private val bcScope    = prepareScope(ChannelScopes.discardCurrent)

    private var puppet       : S                       = _
    private var puppetWrapper: S with PuppetWrapper[S] = _

    override def getPuppet: S = puppet

    override def getPuppetWrapper: S with PuppetWrapper[S] = puppetWrapper

    override def sendInvokeAndWaitResult[R](methodName: String, args: Array[Any]): R = {
        AppLogger.debug(s"Remotely invoking method $methodName(${args.mkString(",")})")
        val result = channel.makeRequest(ownerScope)
                .addPacket(ObjectPacket((methodName, Array(args: _*))))
                .submit()
                .nextResponse
                .nextPacket[RefPacket[R]].value
        result match {
            //FIXME ambiguity with broadcast method invocation.
            case ThrowableWrapper(e) => throw new RemoteInvocationFailedException(s"Invocation of method $methodName with arguments '${args.mkString(", ")}' failed.", e)
            case result              => result
        }
    }

    override def sendInvoke(methodId: Int, args: Array[Any]): Unit = {
        val desc = puppetDescription.getMethodDesc(methodId).get
        AppLogger.debug(s"Remotely invoking method ${desc.method.getName}(${args.mkString(",")})")

        if (desc.isHidden)
            channel.makeRequest(ownerScope)
                    .addPacket(ObjectPacket((methodId, Array(args: _*))))
                    .submit()
                    .detach()
    }

    override def sendFieldUpdate(fieldId: Int, newValue: Any): Unit = {
        AppLogger.vDebug(s"Remotely associating field '${
            puppetDescription.getFieldDesc(fieldId).get.field.getName
        }' to value $newValue.")
        channel.makeRequest(bcScope)
                .addPacket(ObjectPacket((fieldId, newValue)))
                .submit()
                .detach()
    }

    override def sendPuppetUpdate(newVersion: S): Unit = {
        //TODO optimize, directly send the newVersion object to copy paste instead of all its fields.
        puppetDescription.foreachFields(fieldDesc => if (!fieldDesc.isHidden) {
            sendFieldUpdate(fieldDesc.fieldID, fieldDesc.field.get(newVersion))
        })
    }

    override def init(wrapper: S with PuppetWrapper[S], puppet: S): Unit = {
        if (this.puppet != null || this.puppetWrapper != null) {
            throw new IllegalStateException("This Puppeteer already controls a puppet instance !")
        }
        this.puppetWrapper = wrapper
        this.puppet = puppet
    }

    private def prepareScope(factory: ScopeFactory[_ <: ChannelScope]): ChannelScope = {
        if (channel == null)
            return null
        val writer = channel.traffic.newWriter(channel.identifier)
        val scope  = factory.apply(writer)
        presence.drainAllDefaultAttributes(scope)
        scope.addDefaultAttribute("id", puppeteerDescription.objectID)
    }

}
