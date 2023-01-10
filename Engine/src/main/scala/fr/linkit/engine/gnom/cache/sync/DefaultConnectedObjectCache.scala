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

package fr.linkit.engine.gnom.cache.sync

import fr.linkit.api.application.resource.local.LocalFolder
import fr.linkit.api.gnom.cache.SharedCache.CacheInfo
import fr.linkit.api.gnom.cache.sync._
import fr.linkit.api.gnom.cache.sync.contract.StructureContract
import fr.linkit.api.gnom.cache.sync.contract.behavior.ConnectedObjectContext
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.cache.sync.contract.level.{MirrorableSyncLevel, SyncLevel}
import fr.linkit.api.gnom.cache.sync.generation.SyncClassCenter
import fr.linkit.api.gnom.cache.sync.instantiation.{SyncInstanceCreator, SyncObjectInstantiationException, SyncObjectInstantiator}
import fr.linkit.api.gnom.cache.sync.invocation.InvocationChoreographer
import fr.linkit.api.gnom.cache.traffic.CachePacketChannel
import fr.linkit.api.gnom.cache.traffic.handler.{CacheAttachHandler, CacheContentHandler}
import fr.linkit.api.gnom.cache.{SharedCacheFactory, SharedCacheReference}
import fr.linkit.api.gnom.network.tag.{Current, EngineSelector, NetworkFriendlyEngineTag, UniqueTag}
import fr.linkit.api.gnom.network.{Engine, Network}
import fr.linkit.api.gnom.packet.Packet
import fr.linkit.api.gnom.packet.channel.request.{RequestPacketBundle, Submitter}
import fr.linkit.api.gnom.referencing.NamedIdentifier
import fr.linkit.api.gnom.referencing.linker.NetworkObjectLinker
import fr.linkit.api.gnom.referencing.presence.NetworkPresenceHandler
import fr.linkit.api.gnom.referencing.traffic.{ObjectManagementChannel, TrafficInterestedNPH}
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.AbstractSharedCache
import fr.linkit.engine.gnom.cache.sync.DefaultConnectedObjectCache.FirstFloorObjectProfile
import fr.linkit.engine.gnom.cache.sync.contract.behavior.SyncObjectContractFactory
import fr.linkit.engine.gnom.cache.sync.contract.descriptor.EmptyContractDescriptorData
import fr.linkit.engine.gnom.cache.sync.env._
import fr.linkit.engine.gnom.cache.sync.env.node._
import fr.linkit.engine.gnom.cache.sync.generation.{DefaultSyncClassCenter, SyncClassStorageResource}
import fr.linkit.engine.gnom.cache.sync.instantiation.InstanceWrapper
import fr.linkit.engine.gnom.cache.sync.invokation.UsageConnectedObjectContext
import fr.linkit.engine.gnom.cache.sync.invokation.local.ObjectChip
import fr.linkit.engine.gnom.cache.sync.invokation.remote.{InvocationPacket, ObjectPuppeteer}
import fr.linkit.engine.gnom.packet.fundamental.EmptyPacket
import fr.linkit.engine.gnom.packet.fundamental.RefPacket.{AnyRefPacket, ObjectPacket}
import fr.linkit.engine.gnom.persistence.obj.NetworkObjectReferencesLocks
import fr.linkit.engine.internal.debug.{ConnectedObjectCreationStep, Debugger}

import scala.reflect.ClassTag
import scala.util.control.NonFatal

class DefaultConnectedObjectCache[A <: AnyRef] protected(channel              : CachePacketChannel,
                                                         classCenter          : SyncClassCenter,
                                                         override val contract: ContractDescriptorData,
                                                         selector             : EngineSelector,
                                                         defaultPool          : Procrastinator,
                                                         cacheManagerLinker   : NetworkPresenceHandler[SharedCacheReference],
                                                         omc                  : ObjectManagementChannel)
        extends AbstractSharedCache(channel) with InternalConnectedObjectCache[A] {


    protected val contractFactory = new SyncObjectContractFactory(contract)
    override  val registry        = new ConnectedObjectRegistry[A](cacheManagerLinker, omc, defaultPool, channel, DefaultInstantiator, this, info, selector)

    contract.precompile(classCenter)
    channel.setHandler(CacheCenterHandler)


    override def syncObject(id: Int, creator: SyncInstanceCreator[_ <: A]): A with SynchronizedObject[A] = {
        AppLoggers.ConnObj.debug(s"Creating connected object at $reference with id $id.")
        makeSyncObject(id, creator, false)
    }

    override def mirrorObject(id: Int, creator: SyncInstanceCreator[_ <: A]): A with SynchronizedObject[A] = {
        AppLoggers.ConnObj.debug(s"Creating mirrored object at $reference with id $id.")
        makeSyncObject(id, creator, true)
    }


    override def findObject(id: Int): Option[A with SynchronizedObject[A]] = {
        registry.firstLayer.findCompanion(id).map(_.obj)
    }

    override def isRegistered(id: Int): Boolean = {
        registry.firstLayer.findCompanionLocal(id).isDefined || {
            val ownerNT = selector.retrieveNT(info.ownerTag)
            val ref     = ConnectedObjectReference(info.family, info.cacheID, ownerNT, true, id)
            registry.isPresentOnEngine(info.ownerTag, ref)
        }
    }

    override def getContract[B <: AnyRef](creator: SyncInstanceCreator[B],
                                          context: ConnectedObjectContext): StructureContract[B] = {
        contractFactory.getContract[B](creator.syncClassDef.mainClass.asInstanceOf[Class[B]], context)
    }

    private def makeSyncObject(id: Int, creator: SyncInstanceCreator[_ <: A], mirror: Boolean): A with SynchronizedObject[A] = {
        val identifier = NamedIdentifier(creator.syncClassDef.mainClass.getSimpleName, id)

        Debugger.push {
            val currentNT = selector.retrieveNT(Current)
            ConnectedObjectCreationStep(ConnectedObjectReference(info.family, info.cacheID, currentNT, true, identifier))
        }
        val node = registry.firstLayer.register(identifier, Current, creator.asInstanceOf[SyncInstanceCreator[A]], mirror)
        Debugger.pop()
        node.obj
    }

    private def newChippedObjectData[B <: AnyRef](level: SyncLevel, req: ChippedObjectNodeDataRequest[B]): ChippedObjectCompanionData[B] = {
        import req._
        val ownerNT       = req.ownerTag
        val originClass   = connectedObject.getClassDef.mainClass.asInstanceOf[Class[B]]
        val choreographer = new InvocationChoreographer()
        val reference     = ConnectedObjectReference(info.family, info.cacheID, ownerNT, req.firstFloor, id)
        val presence      = registry.getPresence(reference)
        val context       = UsageConnectedObjectContext(ownerNT, connectedObject.getClassDef, level, choreographer, selector)
        val contract      = contractFactory.getContract[B](originClass, context)
        val chip          = ObjectChip[B](contract, selector, channel.traffic.connection, connectedObject)
        new ChippedObjectCompanionData[B](
            selector, chip, contract, choreographer, registry.secondLayer, connectedObject,
        )(new CompanionData[B](reference, presence, req.ownerTag, selector))
    }

    private def newSyncObjectData[B <: AnyRef](req: SyncNodeDataRequest[B]): SyncObjectCompanionData[B] = {
        val chippedData = newChippedObjectData[B](req.syncLevel, req)
        val puppeteer   = new ObjectPuppeteer[B](channel, chippedData.reference)
        new SyncObjectCompanionData[B](puppeteer, req.syncObject, req.syncLevel)(chippedData)
    }

    private def newRegularNodeData[B <: AnyRef](req: NormalNodeDataRequest[B]): CompanionData[B] = {
        import req._
        val reference = new ConnectedObjectReference(info.family, info.cacheID, req.ownerTag, req.firstFloor, identifier)
        val presence  = registry.getPresence(reference)
        new CompanionData[B](reference, presence, req.ownerTag, selector)
    }

    override def newNodeData[B <: AnyRef, N <: CompanionData[B]](req: NodeDataRequest[B, N]): N = {
        implicit def cast[X](a: Any) = a.asInstanceOf[X]

        req match {
            case req: NormalNodeDataRequest[B]        => newRegularNodeData[B](req)
            case req: SyncNodeDataRequest[B]          => newSyncObjectData[B](req)
            case req: ChippedObjectNodeDataRequest[B] => newChippedObjectData[B](MirrorableSyncLevel.Chipped, req)
        }
    }


    private def handleInvocationPacket(ip: InvocationPacket, bundle: RequestPacketBundle): Unit = {
        val ref = ip.objRef
        if (ref.family != info.family || ref.cacheID != info.cacheID)
            throw new IllegalArgumentException(s"Invocation Packet's targeted object reference is not contained in this cache. (targeted: $ref, current cache location is $reference)")
        val lock = NetworkObjectReferencesLocks.getLock(ref)

        lock.lock()
        val node = try registry.findCompanion(ref) finally lock.unlock()

        val senderTag = bundle.coords.senderTag
        AppLoggers.COInv.trace(s"Handling invocation packet over object ${ip.objRef}. For method with id '${ip.methodID}', expected engine identifier return : '${ip.expectedEngineReturn}'")
        node.fold(AppLoggers.ConnObj.error(s"Could not find sync object node for connected object located at $ref")) {
            case node: TrafficInterestedCompanion[_] => node.handlePacket(ip, senderTag, bundle.responseSubmitter)
            case _                                   =>
                throw new BadRMIRequestException(s"Targeted node MUST extends ${classOf[TrafficInterestedCompanion[_]].getSimpleName} in order to handle a member rmi request.")
        }
    }


    private def handleFirstFloorNodeRequest(id: NamedIdentifier, response: Submitter[Unit]): Unit = {
        registry.firstLayer.findCompanionLocal(id) match {
            case Some(node) =>
                val treeProfile = FirstFloorObjectProfile(id, node.obj, node.ownerTag, node.isMirror)
                response.addPacket(ObjectPacket(treeProfile)).submit()
            case None       =>
                response.addPacket(EmptyPacket).submit()
        }
    }

    private object DefaultInstantiator extends SyncObjectInstantiator {

        override def newSynchronizedInstance[B <: AnyRef](creator: SyncInstanceCreator[B]): B with SynchronizedObject[B] = {
            val syncClass = classCenter.getSyncClass[B](creator.syncClassDef)
            try {
                creator.getInstance(syncClass)
            } catch {
                case NonFatal(e) =>
                    throw new SyncObjectInstantiationException(e.getMessage +
                            s"""\nMaybe the origin class of generated sync class '${syncClass.getName}' has been modified ?
                               | Try to regenerate the sync class of ${creator.syncClassDef.mainClass}.
                               | """.stripMargin, e)
            }
        }

    }

    private object CacheCenterHandler extends CacheContentHandler[CacheRepoContent[A]] with CacheAttachHandler {

        override val lazyContentHandling: Boolean = true

        override def getInitialContent: CacheRepoContent[A] = CacheRepoContent(Array())

        override def getContent: CacheRepoContent[A] = registry.firstLayer.snapshotContent

        override def initializeContent(content: CacheRepoContent[A]): Unit = {
            val array = content.array
            if (array.isEmpty)
                return

            array.foreach(profile => {
                val rootObject = profile.obj
                val owner      = profile.owner
                val treeID     = profile.identifier
                val mirror     = profile.mirror
                registry.firstLayer.register(treeID, owner, new InstanceWrapper[A](rootObject), mirror)
            })
        }

        override def handleBundle(bundle: RequestPacketBundle): Unit = {
            //AppLogger.debug(s"Processing bundle : ${bundle}")
            val response = bundle.packet
            response.nextPacket[Packet] match {
                case ip: InvocationPacket              =>
                    handleInvocationPacket(ip, bundle)
                case AnyRefPacket(id: NamedIdentifier) =>
                    handleFirstFloorNodeRequest(id, bundle.responseSubmitter)

                case ObjectPacket(FirstFloorObjectProfile(profile, obj, owner, mirror)) =>
                    registry
                            .firstLayer
                            .register(profile, owner, new InstanceWrapper[A](obj.asInstanceOf[A with SynchronizedObject[A]]), mirror)
            }
        }

        override def onEngineAttached(engine: Engine): Unit = {
            AppLoggers.ConnObj.debug(s"Engine ${engine} attached to this synchronized object cache")
        }

        override def onEngineDetached(engine: Engine): Unit = {
            AppLoggers.ConnObj.debug(s"Engine ${engine} detached to this synchronized object cache")
        }

        override val objectLinker: Option[NetworkObjectLinker[_ <: SharedCacheReference] with TrafficInterestedNPH] = Some(registry)
    }

}

object DefaultConnectedObjectCache extends ConnectedObjectCacheFactories {

    import SharedCacheFactory.lambdaToFactory

    //value does not have any meaning

    private final val ClassesResourceDirectory = LinkitApplication.getProperty("compilation.working_dir") + "/Classes"

    override def apply[A <: AnyRef : ClassTag]: SharedCacheFactory[ConnectedObjectCache[A]] = {
        apply[A](EmptyContractDescriptorData)
    }

    override def apply[A <: AnyRef : ClassTag](contract: ContractDescriptorData): SharedCacheFactory[ConnectedObjectCache[A]] = {
        lambdaToFactory(classOf[DefaultConnectedObjectCache[A]])((channel) => {
            apply[A](channel, contract, channel.traffic.connection.network)
        })
    }



    private[linkit] def apply[A <: AnyRef : ClassTag](contract: ContractDescriptorData,
                                                      network : Network): SharedCacheFactory[ConnectedObjectCache[A]] = {
        lambdaToFactory(classOf[DefaultConnectedObjectCache[A]])(channel => {
            apply[A](channel, contract, network)
        })
    }

    private def apply[A <: AnyRef : ClassTag](channel : CachePacketChannel,
                                              contract: ContractDescriptorData,
                                              network : Network): ConnectedObjectCache[A] = {
        val app       = channel.manager.network.connection.getApp
        val resources = app.getAppResources.getOrOpen[LocalFolder](ClassesResourceDirectory)
                .getEntry
                .getOrAttachRepresentation[SyncClassStorageResource]("lambdas")
        val generator = new DefaultSyncClassCenter(resources, app.compilerCenter)

        val omc = network match {
            case network: AbstractNetwork => network.traffic.getObjectManagementChannel
            case _                        => throw new UnsupportedOperationException("Cannot retrieve object management channel.")
        }

        val cml                         = channel.manager.getCachesLinker
        val defaultPool: Procrastinator = network.connection
        new DefaultConnectedObjectCache[A](channel, generator, contract, network, defaultPool, cml, omc)
    }

    case class FirstFloorObjectProfile[A <: AnyRef](identifier: NamedIdentifier,
                                                    obj       : A with SynchronizedObject[A],
                                                    owner     : UniqueTag with NetworkFriendlyEngineTag,
                                                    mirror    : Boolean) extends Serializable
}