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

package fr.linkit.engine.connection.cache.obj.tree.node

import fr.linkit.api.connection.cache.obj.invokation.local.Chip
import fr.linkit.api.connection.cache.obj.invokation.remote.Puppeteer
import fr.linkit.api.connection.cache.obj.tree.{NoSuchSyncNodeException, SyncNode, SynchronizedObjectTree}
import fr.linkit.api.connection.cache.obj.{CanNotSynchronizeException, SynchronizedObject}
import fr.linkit.api.connection.packet.channel.request.Submitter
import fr.linkit.engine.connection.cache.obj.RMIExceptionString
import fr.linkit.engine.connection.cache.obj.invokation.remote.InvocationPacket
import fr.linkit.engine.connection.packet.UnexpectedPacketException
import fr.linkit.engine.connection.packet.fundamental.RefPacket
import org.jetbrains.annotations.Nullable

import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class WrapperNode[A <: AnyRef](override val puppeteer: Puppeteer[A], //Remote invocation
                               override val chip: Chip[A], //Reflective invocation
                               val tree: SynchronizedObjectTree[_],
                               val currentIdentifier: String,
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
    private   val presences       = mutable.HashSet[String](ownerID, currentIdentifier)

    def addChild(node: WrapperNode[_]): Unit = {
        if (node.parent ne this)
            throw new CanNotSynchronizeException("Attempted to add a child to this node with a different parent of this node.")
        if (members.contains(node.id))
            throw new IllegalStateException(s"Puppet already exists at ${puppeteer.nodeInfo.nodePath.mkString("/") + s"/$id"}")
        members.put(node.id, node)
    }

    def getChild[B <: AnyRef](id: Int): Option[WrapperNode[B]] = (members.get(id): Any) match {
        case None        => None
        case Some(value) => value match {
            case node: WrapperNode[B] => Some(node)
            case _                    => None
        }
    }

    override def isPresentOnEngine(engineID: String): Boolean = presences.contains(engineID)

    override def putPresence(engineID: String): Unit = presences += engineID

    override def handlePacket(packet: InvocationPacket, senderID: String, response: Submitter[Unit]): Unit = {
        if (!(packet.path sameElements treePath)) {
            val packetPath = packet.path
            if (!packetPath.startsWith(treePath))
                throw UnexpectedPacketException(s"Received invocation packet that does not target this node or this node's children ${packetPath.mkString("/")}.")

            tree.findNode[AnyRef](packetPath.drop(treePath.length))
                    .fold[Unit](throw new NoSuchSyncNodeException(s"Received packet that aims for an unknown puppet children node (${packetPath.mkString("/")})")) {
                        case node: TrafficInterestedSyncNode[_] => node.handlePacket(packet, senderID, response)
                        case _                                  =>
                    }
        }
        makeMemberInvocation(packet, senderID, response)
    }

    private def makeMemberInvocation(packet: InvocationPacket, senderID: String, response: Submitter[Unit]): Unit = {
        Try(chip.callMethod(packet.methodID, packet.params, senderID)) match {
            case Success(value)     => value match {
                case anyRef: AnyRef => handleInvocationResult(anyRef, packet, response)
                case _              => //do not synchronize primitives (note: this is impossible to get an unwrapped primitive but this match is for the scalac logic)
            }
            case Failure(exception) => exception match {
                case NonFatal(e) =>
                    e.printStackTrace()
                    if (packet.expectedEngineIDReturn == currentIdentifier)
                        response.addPacket(RMIExceptionString(e.toString)).submit()
            }
        }
    }

    private def handleInvocationResult(initialResult: AnyRef, packet: InvocationPacket, response: Submitter[Unit]): Unit = {
        var result = initialResult
        if (packet.expectedEngineIDReturn == currentIdentifier) {
            val methodBehavior    = puppeteer.wrapperBehavior.getMethodBehavior(packet.methodID)
            val canSyncReturnType = methodBehavior.get.syncReturnValue
            if (result != null && canSyncReturnType && !result.isInstanceOf[SynchronizedObject[_]]) {
                val id = ThreadLocalRandom.current().nextInt()
                result = tree.insertObject(this, id, result, ownerID).synchronizedObject
            }
            response
                    .addPacket(RefPacket[Any](result))
                    .submit()
        }
    }
}
