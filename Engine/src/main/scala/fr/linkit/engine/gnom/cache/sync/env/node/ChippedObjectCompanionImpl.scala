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

package fr.linkit.engine.gnom.cache.sync.env.node

import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.cache.sync.{ChippedObject, ConnectedObjectReference}
import fr.linkit.api.gnom.network.tag._
import fr.linkit.api.gnom.packet.channel.request.Submitter
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.RMIExceptionString
import fr.linkit.engine.gnom.cache.sync.invokation.remote.InvocationPacket
import fr.linkit.engine.gnom.packet.fundamental.RefPacket
import fr.linkit.engine.internal.debug.{Debugger, ResponseStep}

import java.lang.reflect.InvocationTargetException
import scala.util.control.NonFatal

class ChippedObjectCompanionImpl[A <: AnyRef](data: ChippedObjectCompanionData[A]) extends InternalChippedObjectCompanion[A] {

    //Note: The parent can be of type `UnknownSyncObjectNode`. In this case, this node have an unknown parent
    //and the method `discoverParent(ObjectSyncNodeImpl)` can be called at any time by the system.
    override  val reference    : ConnectedObjectReference                = data.reference
    override  val id           : NamedIdentifier                         = reference.identifier
    override  val chip         : Chip[A]                                 = data.chip
    override  val contract     : StructureContract[A]                    = data.contract
    override  val choreographer: InvocationChoreographer                 = data.choreographer
    /**
     * The identifier of the engine that posted this object.
     */
    override  val ownerTag     : UniqueTag with NetworkFriendlyEngineTag = data.ownerID

    /**
     * This set stores every engine where this object is synchronized.
     * */
    override  val presence     : NetworkObjectPresence                   = data.presence

    private val selector: EngineSelector = data.selector

    import selector._

    override def obj: ChippedObject[A] = data.obj

    override def toString: String = s"node $reference for chipped object ${obj.connected}"

    override def handlePacket(packet   : InvocationPacket,
                              senderTag: NameTag,
                              response : Submitter[Unit]): Unit = {
        makeMemberInvocation(packet, senderTag, response)
    }

    private def makeMemberInvocation(packet  : InvocationPacket,
                                     senderNT: NameTag,
                                     response: Submitter[Unit]): Unit = {
        val params               = packet.params
        val expectedEngineReturn = packet.expectedEngineReturn

        def handleException(e: Throwable): Unit = {
            e.printStackTrace()
            val ex = if (e.isInstanceOf[InvocationTargetException]) e.getCause else e
            if (expectedEngineReturn != null && expectedEngineReturn <=> Current)
                handleRemoteInvocationException(response, ex)
        }

        chip.callMethod(packet.methodID, params, senderNT)(handleException, result => try {
            handleInvocationResult(result.asInstanceOf[AnyRef], senderNT, packet, response)
        } catch {
            case NonFatal(e) => handleException(e)
        })

    }

    private def handleRemoteInvocationException(response: Submitter[Unit], t: Throwable): Unit = {
        var ex = t
        val sb = new java.lang.StringBuilder(ex.toString).append("\n")
        while (ex != null && (ex.getCause ne ex)) {
            sb.append("caused by: ")
                    .append(ex.toString)
                    .append("\n")
            ex = ex.getCause
        }
        response.addPacket(RMIExceptionString(sb.toString)).submit()
    }

    private def handleInvocationResult(initialResult: AnyRef,
                                       callerNT     : NameTag,
                                       packet       : InvocationPacket,
                                       response     : Submitter[Unit]): Unit = {
        var result: Any = initialResult

        result = if (initialResult == null) null else {
            val methodContract = contract.findMethodContract[Any](packet.methodID).getOrElse {
                throw new NoSuchElementException(s"Could not find method contract with identifier #$id for ${contract.clazz}.")
            }
            methodContract.handleInvocationResult(initialResult)((ref, registrationLevel) => {
                tree.insertObject(this, ref.asInstanceOf[AnyRef], ownerTag, registrationLevel).obj
            })
        }
        val expectedEngineReturn = packet.expectedEngineReturn
        if (expectedEngineReturn != null && Current <=> expectedEngineReturn) {
            Debugger.push(ResponseStep("rmi", Current, null)) //TODO replace null by the channel's reference
            response
                    .addPacket(RefPacket[Any](result))
                    .submit()
            Debugger.pop()
        }
    }

}
