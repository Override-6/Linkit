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

package fr.linkit.engine.connection.cache.repo

import fr.linkit.api.connection.cache.repo._
import fr.linkit.api.connection.cache.repo.description.{PuppetDescription, PuppetDescriptions, PuppeteerDescription}
import fr.linkit.api.connection.cache.repo.generation.PuppetWrapperGenerator
import fr.linkit.api.connection.cache.{CacheContent, InternalSharedCache, SharedCacheFactory, SharedCacheManager}
import fr.linkit.api.connection.network.Engine
import fr.linkit.api.connection.packet.Packet
import fr.linkit.api.connection.packet.traffic.PacketInjectableContainer
import fr.linkit.api.local.system.AppLogger
import fr.linkit.engine.connection.cache.AbstractSharedCache
import fr.linkit.engine.connection.cache.repo.DefaultEngineObjectCenter.{FieldRestorer, PuppetProfile}
import fr.linkit.engine.connection.cache.repo.generation.{PuppetWrapperClassGenerator, WrapperInstantiator, WrappersClassResource}
import fr.linkit.engine.connection.cache.repo.tree.{DefaultPuppetCenter, MemberSyncNode, PuppetNode}
import fr.linkit.engine.connection.packet.fundamental.RefPacket.{ObjectPacket, StringPacket}
import fr.linkit.engine.connection.packet.traffic.ChannelScopes
import fr.linkit.engine.connection.packet.traffic.channel.request.{RequestBundle, RequestPacketChannel}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.resource.external.LocalResourceFolder._
import fr.linkit.engine.local.utils.ScalaUtils

import java.lang.reflect.Field
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

class DefaultEngineObjectCenter[A <: Serializable](handler: SharedCacheManager,
                                                   cacheID: Int,
                                                   channel: RequestPacketChannel,
                                                   generator: PuppetWrapperGenerator,
                                                   override val descriptions: PuppetDescriptions)
        extends AbstractSharedCache(handler, cacheID, channel) with EngineObjectCenter[A] {

    override val center                = new DefaultPuppetCenter[A]
    private  val fieldRestorer         = new FieldRestorer
    private  val supportIdentifier     = channel.traffic.supportIdentifier
    private  val generatedClassesNames = mutable.HashSet.empty[String]

    override def getPuppetDescription[B <: A : ClassTag : TypeTag]: PuppetDescription[B] = {
        descriptions.getDescription[B]
    }

    override def getPuppetDescFromClass[B](clazz: Class[B]): PuppetDescription[B] = {
        descriptions.getDescFromClass(clazz)
    }

    override def postObject[B <: A : ClassTag : TypeTag](id: Int, snapshot: B): B with PuppetWrapper[B] = {
        ensureNotWrapped(snapshot)
        if (isRegistered(id))
            throw new ObjectAlreadyPostException(s"Another object with id '$id' figures in the repo's list.")

        val path    = Array(id)
        val wrapper = localRegisterRemotePuppet[B](Array(id), supportIdentifier, snapshot)
        channel.makeRequest(ChannelScopes.discardCurrent)
                .addPacket(ObjectPacket(PuppetProfile(path, snapshot, supportIdentifier)))
                .putAllAttributes(this)
                .submit()

        wrapper
    }

    override def findObject[B <: A](id: Int): Option[B with PuppetWrapper[B]] = {
        center.getNode[B](Array(id)).map(_.puppeteer.getPuppetWrapper)
    }

    override def isRegistered(id: Int): Boolean = {
        isRegistered(Array(id))
    }

    override def initPuppetWrapper[B <: A](wrapper: B with PuppetWrapper[B]): Unit = {
        if (wrapper.isInitialized)
            return
        val puppeteerDesc = wrapper.getPuppeteerDescription
        val wrappedClass = wrapper.getWrappedClass.asInstanceOf[Class[B]]
        val puppeteer     = new SimplePuppeteer[B](channel, this, puppeteerDesc, getPuppetDescFromClass[B](wrappedClass))
        wrapper.initPuppeteer(puppeteer)
    }

    private def isRegistered(path: Array[Int]): Boolean = {
        center.getNode(path).isDefined
    }

    private def ensureNotWrapped(any: Any): Unit = {
        if (any.isInstanceOf[PuppetWrapper[_]])
            throw new IllegalPuppetException("This object is already wrapped.")
    }

    private def genPuppetWrapper[B: ClassTag](puppeteer: Puppeteer[B], puppet: B): B with PuppetWrapper[B] = {
        val wrapperClass    = generator.getPuppetClass[B](puppet.getClass.asInstanceOf[Class[B]])
        val puppetClassName = puppet.getClass.getName
        if (!generatedClassesNames.contains(puppetClassName)) {
            channel.makeRequest(ChannelScopes.broadcast)
                    .addPacket(StringPacket(puppetClassName))
                    .submit()
            generatedClassesNames += puppetClassName
        }
        val instance = WrapperInstantiator.instantiateFromOrigin[B](wrapperClass, puppet)
        instance.initPuppeteer(puppeteer)
        instance
    }

    override protected def handleBundle(bundle: RequestBundle): Unit = {
        AppLogger.vDebug(s"Processing bundle : ${bundle}")
        val response = bundle.packet
        val owner    = bundle.coords.senderID
        response.nextPacket[Packet] match {
            case ObjectPacket(profile: PuppetProfile[A]) =>
                if (!isRegistered(profile.treeViewPath)) {
                    val puppet = profile.puppet
                    fieldRestorer.restoreFields(puppet)
                    localRegisterRemotePuppet(profile.treeViewPath, owner, puppet)(ClassTag(puppet.getClass))
                }
            case StringPacket(puppetClassName)           =>
                generator.getPuppetClass(Class.forName(puppetClassName))
            case ip: InvocationPacket                    =>
                val path = ip.path
                val node = center.getNode(path)
                node.fold(AppLogger.error(s"Could not find puppet node at path ${path.mkString("$", " -> ", "")}")) {
                    case node: MemberSyncNode[_] => node.handlePacket(ip, bundle.responseSubmitter)
                    case _                       =>
                        throw new BadRMIRequestException(s"Targeted node MUST extends ${classOf[MemberSyncNode[_]].getSimpleName} in order to handle a member rmi request.")
                }
        }
    }

    override def setContent(content: CacheContent): Unit = {
        content match {
            case content: CacheRepoContent[A] => setRepoContent(content)
            case _                            => throw new IllegalArgumentException(s"Received unknown content '$content'")
        }
    }

    override def snapshotContent: CacheRepoContent[A] = center.snapshotContent

    override def genSynchronizedObject[B: ClassTag](treeViewPath: Array[Int],
                                                    obj: B,
                                                    owner: String,
                                                    descriptions: PuppetDescriptions)(foreachSyncObjAction: (PuppetWrapper[_ <: Any], Array[Int]) => Unit): B with PuppetWrapper[B] = {
        ensureNotWrapped(obj)

        if (treeViewPath.isEmpty)
            throw new IllegalArgumentException("Synchronized object's tree view path must not be empty.")

        if (obj == null)
            throw new NullPointerException

        val puppetDesc    = descriptions.getDescFromClass[B](obj.getClass.asInstanceOf[Class[B]])
        val puppeteerDesc = PuppeteerDescription(family, cacheID, owner, treeViewPath)
        val puppeteer     = new SimplePuppeteer[B](channel, this, puppeteerDesc, puppetDesc)
        val wrapper       = genPuppetWrapper[B](puppeteer, obj)
        foreachSyncObjAction(wrapper, treeViewPath)
        val mirror = runtimeMirror(puppetDesc.loader).reflect(wrapper)
        for (desc <- puppetDesc.listFields() if desc.isSynchronized) {

            val id            = ThreadLocalRandom.current().nextInt()
            val field         = desc.fieldGetter
            val fieldValue    = mirror.reflectMethod(field)()
            val childTreePath = treeViewPath ++ Array(id)
            val syncObject    = genSynchronizedObject(childTreePath, fieldValue, owner, descriptions)(foreachSyncObjAction)(ClassTag(fieldValue.getClass))
            mirror.reflectMethod(field.setter.asMethod)(syncObject)
        }
        wrapper
    }

    private def setRepoContent(content: CacheRepoContent[A]): Unit = {
        if (content.array.isEmpty)
            return
        val array = content.array

        array.foreach(profile => {
            val puppet = profile.puppet
            val owner  = profile.owner
            val path   = profile.treeViewPath
            //it's an object that must be chipped by this current repo cache (owner is the same as current identifier)
            if (owner == supportIdentifier) {
                center.getNode[A](path).fold {
                    throw new IllegalArgumentException(s"Unknown local object of path '${path.mkString("$", " -> ", "")}'")
                }(_.chip.updateAllFields(puppet))
            }
            //it's an object that must be remotely controlled because it is chipped by another objects cache.
            else if (!isRegistered(path)) {
                localRegisterRemotePuppet(path, owner, puppet)(ClassTag(puppet.getClass))
            }
        })
    }

    private def localRegisterRemotePuppet[B <: A : ClassTag](path: Array[Int], owner: String, puppet: B): B with PuppetWrapper[B] = {
        val isIntended = owner == supportIdentifier

        var parent        = center.getNode[B](path)
        val wrappedPuppet = genSynchronizedObject(path, puppet, owner, descriptions) {
            (wrapper, childPath) =>
                val id          = childPath.last
                val description = wrapper.getPuppetDescription.asInstanceOf[PuppetDescription[B]]
                parent.fold {
                    val rootWrapper = wrapper.asInstanceOf[B with PuppetWrapper[B]]
                    val chip        = ObjectChip[B](owner, description, rootWrapper)
                    val parentNode  = new PuppetNode[B](rootWrapper.getPuppeteer, chip, descriptions, isIntended, id, null)
                    center.addPuppet(path, _ => parentNode)
                    parent = Some(parentNode)
                } { parentNode =>
                    parentNode.getGrandChild(childPath.drop(path.length).dropRight(1))
                            .fold(throw new NoSuchPuppetException(s"Puppet Node not found in path ${childPath.mkString("$", " -> ", "")}")) {
                                parent =>
                                    val chip      = ObjectChip[Any](owner, description, wrapper.asInstanceOf[Any with PuppetWrapper[Any]])
                                    val puppeteer = wrapper.getPuppeteer.asInstanceOf[Puppeteer[Any]]
                                    parent.addChild(id, new PuppetNode(puppeteer, chip, descriptions, isIntended, id, _))
                            }
                }
        }

        wrappedPuppet
    }
}

object DefaultEngineObjectCenter {

    private val ClassesResourceDirectory = LinkitApplication.getProperty("compilation.working_dir.classes")

    def apply[A <: Serializable : ClassTag](descriptions: PuppetDescriptions = PuppetDescriptions.getDefault): SharedCacheFactory[DefaultEngineObjectCenter[A] with InternalSharedCache] = {
        (handler: SharedCacheManager, identifier: Int, container: PacketInjectableContainer) => {
            val channel   = container.getInjectable(5, ChannelScopes.discardCurrent, RequestPacketChannel)
            val context   = handler.network.connection.getApp
            val resources = context.getAppResources.getOrOpenThenRepresent[WrappersClassResource](ClassesResourceDirectory)
            val generator = new PuppetWrapperClassGenerator(context.compilerCenter, resources)

            new DefaultEngineObjectCenter[A](handler, identifier, channel, generator, descriptions)
        }
    }

    class FieldRestorer {

        private val values = mutable.HashMap.empty[Field, Any]

        def putValue(field: Field, value: Any): Unit = {
            field.setAccessible(true)
            values.put(field, value)
        }

        def restoreFields(any: Any): Unit = {
            val clazz = any.getClass

            listRestorableFields(clazz).foreach(field => {
                ScalaUtils.setValue(any, field, values(field))
            })
        }

        private def listRestorableFields(clazz: Class[_]): Array[Field] = {
            if (clazz == null)
                return Array.empty
            clazz.getDeclaredFields.filter(values.contains) ++
                    listRestorableFields(clazz.getSuperclass) ++
                    clazz.getInterfaces.flatMap(listRestorableFields)
        }
    }

    case class PuppetProfile[A <: Serializable](treeViewPath: Array[Int], puppet: A, owner: String) extends Serializable

}