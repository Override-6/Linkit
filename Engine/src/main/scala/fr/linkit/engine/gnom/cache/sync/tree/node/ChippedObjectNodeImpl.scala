package fr.linkit.engine.gnom.cache.sync.tree.node

import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.contract.description.SyncClassDef
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.sync.invocation.local.Chip
import fr.linkit.api.gnom.cache.sync.tree.{ConnectedObjectNode, NoSuchSyncNodeException}
import fr.linkit.api.gnom.cache.sync.{CanNotSynchronizeException, ChippedObject, ConnectedObjectReference}
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.gnom.packet.channel.request.Submitter
import fr.linkit.api.gnom.reference.presence.NetworkObjectPresence
import fr.linkit.engine.gnom.cache.sync.RMIExceptionString
import fr.linkit.engine.gnom.cache.sync.invokation.remote.InvocationPacket
import fr.linkit.engine.gnom.cache.sync.tree.DefaultSynchronizedObjectTree
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.gnom.packet.fundamental.RefPacket

import java.lang.reflect.InvocationTargetException
import scala.collection.mutable
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class ChippedObjectNodeImpl[A <: AnyRef](data: ChippedObjectNodeData[A]) extends InternalChippedObjectNode[A] {

    //Note: The parent can be of type `UnknownSyncObjectNode`. In this case, this node have an unknown parent
    //and the method `discoverParent(ObjectSyncNodeImpl)` can be called at any time by the system.
    private var parent0            : ConnectedObjectNode[_]           = data.parent.orNull
    override  val reference        : ConnectedObjectReference         = data.reference
    override  val id               : Int                              = reference.nodePath.last
    override  val chip             : Chip[A]                          = data.chip
    override  val tree             : DefaultSynchronizedObjectTree[_] = data.tree
    override  val contract         : StructureContract[A]             = data.contract
    override  val choreographer    : InvocationChoreographer          = data.choreographer
    /**
     * The identifier of the engine that posted this object.
     */
    override  val ownerID          : String                           = data.ownerID
    /**
     * This map contains all the synchronized object of the parent object
     * including method return values and parameters and class fields
     * */
    protected val childs                                              = new mutable.HashMap[Int, MutableNode[_]]
    private   val currentIdentifier: String                           = data.currentIdentifier
    /**
     * This set stores every engine where this object is synchronized.
     * */
    override  val objectPresence   : NetworkObjectPresence            = data.presence

    override def obj: ChippedObject[A] = data.obj

    override def parent: ConnectedObjectNode[_] = parent0

    /**
     * Replace the unknown parent by the known one.
     * @throws IllegalStateException if the current parent is not an [[UnknownObjectSyncNode]]
     * */
    override def discoverParent(node: ObjectSyncNodeImpl[_]): Unit = {
        if (!parent.isInstanceOf[UnknownObjectSyncNode])
            throw new IllegalStateException("Parent already known !")

        this.parent0 = parent0
    }

    override def addChild(node: MutableNode[_]): Unit = {
        if (node.parent ne this)
            throw new CanNotSynchronizeException("Attempted to add a child to this node that does not define this node as its parent.")
        if (node eq this)
            throw new IllegalArgumentException("can't add self as child")

        def put(): Unit = childs.put(node.id, node)

        childs.get(node.id) match {
            case Some(value) => value match {
                case _: UnknownObjectSyncNode    => put()
                case _: ChippedObjectNodeImpl[_] =>
                    throw new IllegalStateException(s"An Object Node already exists at ${reference.nodePath.mkString("/") + s"/$id"}")
            }
            case None        => put()
        }
    }

    override def toString: String = s"node $reference for chipped object ${obj.connected}"

    def getChild[B <: AnyRef](id: Int): Option[MutableNode[B]] = {
        (childs.get(id): Any).asInstanceOf[Option[MutableNode[B]]]
    }

    override def handlePacket(packet: InvocationPacket, senderID: String, response: Submitter[Unit]): Unit = {
        if (!(packet.path sameElements nodePath)) {
            val packetPath = packet.path
            if (!packetPath.startsWith(nodePath))
                throw UnexpectedPacketException(s"Received invocation packet that does not target this node or this node's children ${packetPath.mkString("/")}.")

            tree.findNode[AnyRef](packetPath.drop(nodePath.length))
                    .fold[Unit](throw new NoSuchSyncNodeException(s"Received packet that aims for an unknown puppet children node (${packetPath.mkString("/")})")) {
                        case node: TrafficInterestedNode[_] => node.handlePacket(packet, senderID, response)
                        case _                              =>
                    }
        }
        makeMemberInvocation(packet, senderID, response)
    }

    private def makeMemberInvocation(packet: InvocationPacket, senderID: String, response: Submitter[Unit]): Unit = {
        val executor = data.network.findEngine(senderID).orNull
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
            ??? //tree.insertObject(this, param, currentIdentifier).obj
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

        result = if (initialResult != null) {
            val methodContract = contract.findMethodContract[Any](packet.methodID).getOrElse {
                throw new NoSuchElementException(s"Could not find method contract with identifier #$id for ${contract.clazz}.")
            }
            methodContract.handleInvocationResult(initialResult, engine)((ref, registrationKind) => {
                tree.insertObject(this, ref, ownerID, registrationKind).obj
            })
        } else null
        if (packet.expectedEngineIDReturn == currentIdentifier) {
            response
                    .addPacket(RefPacket[Any](result))
                    .submit()
        }
    }
}
