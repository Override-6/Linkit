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

package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.invokation.local.Chip
import fr.linkit.api.gnom.cache.sync.invokation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree.{NoSuchSyncNodeException, SyncNode, SyncObjectReference, SynchronizedObjectTree}
import fr.linkit.api.gnom.cache.sync.{CanNotSynchronizeException, SynchronizedObject}
import fr.linkit.api.gnom.packet.channel.request.Submitter
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.RMIExceptionString
import fr.linkit.engine.gnom.cache.sync.invokation.remote.InvocationPacket
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.gnom.packet.fundamental.RefPacket
import org.jetbrains.annotations.Nullable

import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class ObjectSyncNode[A <: AnyRef](@Nullable override val parent: SyncNode[_],
                                  data: ObjectNodeData[A]) extends TrafficInterestedSyncNode[A] {

    override  val location          : SyncObjectReference       = data.location
    override  val tree              : SynchronizedObjectTree[_] = data.tree
    override  val puppeteer         : Puppeteer[A]                 = data.puppeteer
    override  val chip              : Chip[A]                      = data.chip
    override  val synchronizedObject: A with SynchronizedObject[A] = data.synchronizedObject
    override  val id                : Int                          = location.nodePath.last
    /**
     * The identifier of the engine that posted this object.
     */
    override  val ownerID           : String                       = puppeteer.ownerID
    /**
     * This map contains all the synchronized object of the parent object
     * including method return values and parameters and class fields
     * */
    protected val members                                          = new mutable.HashMap[Int, ObjectSyncNode[_]]
    private   val currentIdentifier : String                       = data.currentIdentifier
    /**
     * This set stores every engine where this object is synchronized.
     * */
    private   val presences                                 = mutable.HashSet[String](ownerID, currentIdentifier)
    override  val objectPresence    : NetworkObjectPresence = null

    synchronizedObject.initialize(this)

    def addChild(node: ObjectSyncNode[_]): Unit = {
        if (node.parent ne this)
            throw new CanNotSynchronizeException("Attempted to add a child to this node with a different parent of this node.")
        if (members.contains(node.id))
            throw new IllegalStateException(s"Puppet already exists at ${puppeteer.nodeLocation.nodePath.mkString("/") + s"/$id"}")
        members.put(node.id, node)
    }

    def getChild[B <: AnyRef](id: Int): Option[ObjectSyncNode[B]] = (members.get(id): Any) match {
        case None        => None
        case Some(value) => value match {
            case node: ObjectSyncNode[B] => Some(node)
            case _                       => None
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
            val methodBehavior    = puppeteer.objectBehavior.getMethodBehavior(packet.methodID)
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
