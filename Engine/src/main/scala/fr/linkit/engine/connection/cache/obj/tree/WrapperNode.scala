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

package fr.linkit.engine.connection.cache.obj.tree

import java.util.concurrent.ThreadLocalRandom

import fr.linkit.api.connection.cache.obj.tree.{NoSuchWrapperNodeException, SyncNode, SynchronizedObjectTree}
import fr.linkit.api.connection.cache.obj.{Chip, IllegalObjectWrapperException, PuppetWrapper, Puppeteer}
import fr.linkit.engine.connection.cache.obj.invokation.remote.InvocationPacket
import fr.linkit.engine.connection.packet.UnexpectedPacketException
import fr.linkit.engine.connection.packet.fundamental.RefPacket
import fr.linkit.engine.connection.packet.traffic.channel.request.ResponseSubmitter
import org.jetbrains.annotations.Nullable

import scala.collection.mutable

class WrapperNode[A](override val puppeteer: Puppeteer[A], //Remote invocation
                     override val chip: Chip[A], //Reflective invocation
                     val tree: SynchronizedObjectTree[_],
                     val platformIdentifier: String,
                     override val id: Int,
                     @Nullable override val parent: SyncNode[_]) extends TrafficInterestedSyncNode[A] {

    /**
     * The identifier of the engine that posted this object.
     */
    override  val ownerID: String = puppeteer.ownerID
    /**
     * This map contains all the synchronized object of the parent object
     * including method return values and parameters and class fields
     * */
    protected val members         = new mutable.HashMap[Int, WrapperNode[_]]
    /**
     * This set stores every engine where this object is synchronized.
     * */
    private   val presences       = mutable.HashSet[String](ownerID, platformIdentifier)

    def addChild(node: WrapperNode[_]): Unit = {
        if (node.parent ne this)
            throw new IllegalObjectWrapperException("Attempted to add a child to this node with a different parent of this node.")
        val last = members.put(node.id, node)
        if (last.isDefined)
            throw new IllegalStateException(s"Puppet already exists at ${puppeteer.puppeteerInfo.nodePath.mkString("$", " -> ", s" -> ${node.id}")}")
    }

    def getChild[B](id: Int): Option[WrapperNode[B]] = members.get(id) match {
        case None        => None
        case Some(value) => value match {
            case node: WrapperNode[B] => Some(node)
            case _                    => None
        }
    }

    override def isPresentOnEngine(engineID: String): Boolean = presences.contains(engineID)

    override def putPresence(engineID: String): Unit = presences += engineID

    override def handlePacket(packet: InvocationPacket, response: ResponseSubmitter): Unit = {
        if (!(packet.path sameElements treePath)) {
            val packetPath = packet.path
            if (!packetPath.startsWith(treePath))
                throw UnexpectedPacketException(s"Received invocation packet that does not target this node or this node's children ${packetPath.mkString("$", " -> ", "")}.")

            tree.findNode[Unit](packetPath.drop(treePath.length))
                .fold(throw new NoSuchWrapperNodeException(s"Received packet that aims for an unknown puppet children node (${packetPath.mkString("$", " -> ", "")})")) {
                    case node: TrafficInterestedSyncNode[_] => node.handlePacket(packet, response)
                    case _                                  =>
                }
        }
        makeMemberInvocation(packet, response)
    }

    private def makeMemberInvocation(packet: InvocationPacket, response: ResponseSubmitter): Unit = {
        val methodID = packet.methodID

        var result = chip.callMethod(methodID, packet.params)
        if (packet.expectedEngineIDReturn == platformIdentifier) {
            val methodBehavior    = puppeteer.wrapperBehavior.getMethodBehavior(methodID)
            val canSyncReturnType = methodBehavior.get.syncReturnValue
            if (result != null && canSyncReturnType && !result.isInstanceOf[PuppetWrapper[_]]) {
                val id = ThreadLocalRandom.current().nextInt()
                result = tree.insertObject(this, id, result, ownerID).synchronizedObject
            }
            response
                .addPacket(RefPacket[Any](result))
                .submit()
        }
    }
}
