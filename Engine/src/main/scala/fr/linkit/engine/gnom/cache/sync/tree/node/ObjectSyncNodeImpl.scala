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

import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.cache.sync.invocation.remote.Puppeteer
import fr.linkit.api.gnom.cache.sync.tree._
import fr.linkit.api.gnom.cache.sync.{CanNotSynchronizeException, SyncObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.gnom.packet.channel.request.Submitter
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.invokation.remote.InvocationPacket
import fr.linkit.engine.gnom.cache.sync.tree.DefaultSynchronizedObjectTree
import fr.linkit.engine.gnom.cache.sync.{AbstractSynchronizedObject, IllegalSynchronizedObjectException, RMIExceptionString}
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.gnom.packet.fundamental.RefPacket
import org.jetbrains.annotations.Nullable

import java.lang.reflect.InvocationTargetException
import scala.collection.mutable
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class ObjectSyncNodeImpl[A <: AnyRef](private var parent0: SyncNode[_],
                                      data: ObjectNodeData[A]) extends InternalObjectSyncNode[A] {

    override  val reference         : SyncObjectReference              = data.reference
    override  val contract          : StructureContract[A]             = data.contract
    override  val id                : Int                              = reference.nodePath.last
    override  val chip              : Chip[A]                          = data.chip
    override  val puppeteer         : Puppeteer[A]                     = data.puppeteer
    override  val tree              : DefaultSynchronizedObjectTree[_] = data.tree
    override  val synchronizedObject: A with SynchronizedObject[A]     = data.synchronizedObject
    /**
     * The identifier of the engine that posted this object.
     */
    override  val ownerID           : String                           = puppeteer.ownerID
    /**
     * This map contains all the synchronized object of the parent object
     * including method return values and parameters and class fields
     * */
    protected val childs                                               = new mutable.HashMap[Int, MutableSyncNode[_]]
    private   val currentIdentifier : String                           = data.currentIdentifier
    /**
     * This set stores every engine where this object is synchronized.
     * */
    override  val objectPresence    : NetworkObjectPresence            = data.presence
    private   val originRef                                            = data.origin

    initSyncObject()

    override def parent: SyncNode[_] = parent0

    override def discoverParent(node: ObjectSyncNodeImpl[_]): Unit = {
        if (!parent.isInstanceOf[UnknownObjectSyncNode])
            throw new IllegalStateException("Parent already known !")

        this.parent0 = parent0
    }

    override def addChild(node: MutableSyncNode[_]): Unit = {
        if (node.parent ne this)
            throw new CanNotSynchronizeException("Attempted to add a child to this node that does not define this node as its parent.")
        if (node eq this)
            throw new IllegalArgumentException("can't add self as child")

        def put(): Unit = childs.put(node.id, node)

        childs.get(node.id) match {
            case Some(value) => value match {
                case _: UnknownObjectSyncNode => put()
                case _: ObjectSyncNodeImpl[_] =>
                    throw new IllegalStateException(s"A Synchronized Object Node already exists at ${puppeteer.nodeLocation.nodePath.mkString("/") + s"/$id"}")
            }
            case None        => put()
        }
    }

    override def toString: String = s"node $reference for sync object ${synchronizedObject.getClass.getName}"

    def getChild[B <: AnyRef](id: Int): Option[ObjectSyncNodeImpl[B]] = (childs.get(id): Any) match {
        case None        => None
        case Some(value) => value match {
            case node: ObjectSyncNodeImpl[B] => Some(node)
            case _                           => None
        }
    }

    @Nullable
    def getMatchingSyncNode(nonSyncObject: AnyRef): ObjectSyncNode[_ <: AnyRef] = InvocationChoreographer.disableInvocations {
        val origin = if (originRef == null) null else originRef.get()
        if (origin != null && (nonSyncObject eq origin))
            return this

        for (child <- childs.values) {
            val found = child.getMatchingSyncNode(nonSyncObject)
            if (found != null)
                return found
        }
        null
    }

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
        val executor = puppeteer.network.findEngine(senderID).orNull
        val params   = packet.params
        scanParams(params)
        Try(chip.callMethod(packet.methodID, params, executor)) match {
            case Success(value)     => handleInvocationResult(value.asInstanceOf[AnyRef], executor, packet, response)
            case Failure(exception) => exception match {
                case NonFatal(e) =>
                    val ex = if (e.isInstanceOf[InvocationTargetException]) e.getCause else e
                    if (packet.expectedEngineIDReturn == currentIdentifier)
                        handleInvocationException(response, ex)
                    e.printStackTrace()
                case o           => throw o
            }
        }
    }

    private def scanParams(params: Array[Any]): Unit = {
        params.mapInPlace { case param: AnyRef => if (tree.forest.isObjectLinked(param)) {
            tree.insertObject(this, param, currentIdentifier).synchronizedObject
        } else param
        }
    }

    private def handleInvocationException(response: Submitter[Unit], t: Throwable): Unit = {
        var ex = t
        val sb = new StringBuilder(ex.toString).append("\n")
        while (ex != null && (ex.getCause ne ex)) {
            sb.append("caused by: ")
                .append(ex.toString)
                .append("\n")
            ex = ex.getCause
        }
        response.addPacket(RMIExceptionString(sb.toString)).submit()
    }

    private def handleInvocationResult(initialResult: AnyRef, engine: Engine, packet: InvocationPacket, response: Submitter[Unit]): Unit = {
        var result: Any = initialResult
        if (packet.expectedEngineIDReturn == currentIdentifier) {
            val methodContract = contract.findMethodContract[Any](packet.methodID).getOrElse {
                throw new NoSuchElementException(s"Could not find method contract with identifier #$id for ${contract.clazz}.")
            }
            result = methodContract.handleInvocationResult(initialResult, engine)(ref => {
                tree.insertObject(this, ref, ownerID)
                    .synchronizedObject
            })
            response
                .addPacket(RefPacket[Any](result))
                .submit()
        }
    }

    private def initSyncObject(): Unit = {
        synchronizedObject match {
            case sync: AbstractSynchronizedObject[A] => sync.initialize(this)
            case _                                   => throw new IllegalSynchronizedObjectException(
                "Received unknown kind of synchronized object: " +
                    "could not initialize synchronized object because received object does not extends AbstractSynchronizedObject.")
        }
    }

}
