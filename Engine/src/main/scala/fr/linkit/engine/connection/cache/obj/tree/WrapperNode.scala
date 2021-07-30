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

import fr.linkit.api.connection.cache.obj.description.TreeViewBehavior
import fr.linkit.api.connection.cache.obj.tree.SyncNode
import fr.linkit.api.connection.cache.obj.{Chip, PuppetWrapper, Puppeteer}
import fr.linkit.engine.connection.cache.obj.NoSuchPuppetNodeException
import fr.linkit.engine.connection.cache.obj.invokation.remote.InvocationPacket
import fr.linkit.engine.connection.packet.UnexpectedPacketException
import fr.linkit.engine.connection.packet.fundamental.RefPacket
import fr.linkit.engine.connection.packet.traffic.channel.request.ResponseSubmitter
import org.jetbrains.annotations.Nullable

import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable

class WrapperNode[A](override val puppeteer: Puppeteer[A], //Remote invocation
                     override val chip: Chip[A], //Reflective invocation
                     val descriptions: TreeViewBehavior,
                     val platformIdentifier: String,
                     override val id: Int,
                     @Nullable override val parent: SyncNode[_]) extends MemberSyncNode[A] {

    /**
     * The identifier of the engine that posted this object.
     */
    private   val ownerID: String = puppeteer.ownerID
    /**
     * This map contains all the synchronized object of the parent object
     * including method return values and parameters and class fields
     * */
    protected val members         = new mutable.HashMap[Int, SyncNode[_]]
    /**
     * This set stores every engine where this object is synchronized.
     * */
    private   val presences        = mutable.HashSet[String](ownerID, platformIdentifier)

    override def addChild(node: SyncNode[_]): Unit = {
        if (node.parent ne this)
            throw new UnsupportedOperationException("Attempted to add a child to this node with a different parent of this node.")
        val last = members.put(node.id, node)
        if (last.isDefined)
            throw new IllegalStateException(s"Puppet already exists at ${puppeteer.puppeteerInfo.treeViewPath.mkString("$", " -> ", s" -> ${node.id}")}")
    }

    override def getChild[B](id: Int): Option[SyncNode[B]] = members.get(id) match {
        case None        => None
        case Some(value) => value match {
            case node: WrapperNode[B] => Some(node)
            case _                    => None
        }
    }

    override def isPresentOnEngine(engineID: String): Boolean = presences.contains(engineID)

    override def putPresence(engineID: String): Unit = presences += engineID

    override def handlePacket(packet: InvocationPacket, response: ResponseSubmitter): Unit = {
        if (!(packet.path sameElements treeViewPath)) {
            val packetPath = packet.path
            if (!packetPath.startsWith(treeViewPath))
                throw UnexpectedPacketException(s"Received invocation packet that does not target this node or this node's children ${packetPath.mkString("$", " -> ", "")}.")

            getGrandChild(packetPath.drop(treeViewPath.length))
                    .fold(throw new NoSuchPuppetNodeException(s"Received packet that aims for an unknown puppet children node (${packetPath.mkString("$", " -> ", "")})")) {
                        case node: MemberSyncNode[_] => node.handlePacket(packet, response)
                        case _                       =>
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
                val id             = ThreadLocalRandom.current().nextInt()
                val synchronizer   = puppeteer.repo
                val resultNodePath = treeViewPath ++ Array(id)

                result = synchronizer.genSynchronizedObject(resultNodePath, result, ownerID, descriptions)._1
            }
            response
                    .addPacket(RefPacket[Any](result))
                    .submit()
        }
    }
}
