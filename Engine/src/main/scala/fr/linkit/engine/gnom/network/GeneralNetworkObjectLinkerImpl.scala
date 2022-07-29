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

package fr.linkit.engine.gnom.network

import fr.linkit.api.application.ApplicationReference
import fr.linkit.api.application.connection.NetworkConnectionReference
import fr.linkit.api.gnom.cache.SharedCacheManagerReference
import fr.linkit.api.gnom.network.{EngineReference, Network, NetworkReference}
import fr.linkit.api.gnom.packet.channel.request.RequestPacketBundle
import fr.linkit.api.gnom.persistence.obj.TrafficReference
import fr.linkit.api.gnom.referencing._
import fr.linkit.api.gnom.referencing.linker.{DefaultNetworkObjectLinker, GeneralNetworkObjectLinker, InitialisableNetworkObjectLinker, NetworkObjectLinker}
import fr.linkit.api.gnom.referencing.presence.NetworkObjectPresence
import fr.linkit.api.gnom.referencing.traffic.{LinkerRequestBundle, ObjectManagementChannel, TrafficInterestedNPH}
import fr.linkit.engine.gnom.network.GeneralNetworkObjectLinkerImpl.ReferenceAttributeKey
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.gnom.packet.traffic.channel.request.DefaultRequestBundle
import fr.linkit.engine.gnom.referencing.AbstractNetworkPresenceHandler
import fr.linkit.engine.gnom.referencing.presence.SystemNetworkObjectPresence
import fr.linkit.engine.internal.utils.ClassMap

import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}

class GeneralNetworkObjectLinkerImpl(omc: ObjectManagementChannel,
                                     network: Network,
                                     override val cacheNOL: InitialisableNetworkObjectLinker[SharedCacheManagerReference] with TrafficInterestedNPH,
                                     override val trafficNOL: NetworkObjectLinker[TrafficReference] with TrafficInterestedNPH,
                                     override val defaultNOL: DefaultNetworkObjectLinker with TrafficInterestedNPH)
        extends GeneralNetworkObjectLinker {
    
    private val connection   = network.connection
    private val application  = connection.getApp
    private val otherLinkers = new ClassMap[NetworkObjectLinker[NetworkObjectReference]]()
    
    startObjectManagement()
    
    override def addRootLinker[R <: NetworkObjectReference: ClassTag](linker: NetworkObjectLinker[R]): Unit = {
        otherLinkers put (classTag[R].runtimeClass, linker.asInstanceOf[NetworkObjectLinker[NetworkObjectReference]])
    }
    
    /**
     *
     * @param reference the reference to test
     * @return true if a [[NetworkObjectLinker]] (excepted remainingNOL) is able to handle the given reference.
     */
    override def touchesAnyLinker(reference: NetworkObjectReference): Boolean = {
        reference.isInstanceOf[SharedCacheManagerReference] ||
                reference.isInstanceOf[TrafficReference] ||
                reference.isInstanceOf[SystemObjectReference] ||
                otherLinkers.contains(reference.getClass)
    }
    
    override def findRootLinker(reference: NetworkObjectReference): Option[NetworkObjectLinker[_ <: NetworkObjectReference]] = {
        reference match {
            case _: SharedCacheManagerReference => Some(cacheNOL)
            case _: TrafficReference            => Some(trafficNOL)
            case _: SystemObjectReference       => Some(this)
            case _                              => otherLinkers.get(reference.getClass)
        }
    }
    
    override def isPresentOnEngine(engineID: String, reference: NetworkObjectReference): Boolean = reference match {
        case ref: SharedCacheManagerReference => cacheNOL.isPresentOnEngine(engineID, ref)
        case ref: TrafficReference            => trafficNOL.isPresentOnEngine(engineID, ref)
        case _: SystemObjectReference         => true //System references, are guaranteed to be present on every remote engines
        case _                                =>
            otherLinkers.get(reference.getClass) match {
                case Some(linker) => linker.isPresentOnEngine(engineID, reference)
                case None         => defaultNOL.isPresentOnEngine(engineID, reference)
            }
    }
    
    override def getPresence(reference: NetworkObjectReference): NetworkObjectPresence = reference match {
        case ref: SharedCacheManagerReference => cacheNOL.getPresence(ref)
        case ref: TrafficReference            => trafficNOL.getPresence(ref)
        case _: SystemObjectReference         =>
            /* As other NetworkReferences are guaranteed
            * to be present on every engines,
            * The result of their presence on any engine will be always present if they are legit objects.
            */
            SystemNetworkObjectPresence
        case _                                =>
            otherLinkers.get(reference.getClass) match {
                case Some(linker) => linker.getPresence(reference)
                case None         => defaultNOL.getPresence(reference)
            }
    }
    
    override def findObject(reference: NetworkObjectReference): Option[NetworkObject[_ <: NetworkObjectReference]] = reference match {
        case ref: SharedCacheManagerReference => cacheNOL.findObject(ref)
        case ref: TrafficReference            => trafficNOL.findObject(ref)
        case reference: SystemObjectReference => SystemObjectPresenceHandler.findObject(reference)
        case _                                =>
            otherLinkers.get(reference.getClass) match {
                case Some(linker) => linker.findObject(reference)
                case None         => defaultNOL.findObject(reference)
            }
    }
    
    private def handleRequest(request: RequestPacketBundle): Unit = {
        val reference    = request.attributes.getAttribute[NetworkObjectReference](ReferenceAttributeKey).getOrElse {
            throw UnexpectedPacketException(s"Could not find attribute '$ReferenceAttributeKey' in object management request bundle $request")
        }
        val linkerBundle = new DefaultRequestBundle(omc, request.packet, request.coords, request.responseSubmitter) with LinkerRequestBundle {
            override val linkerReference: NetworkObjectReference = reference
        }
        reference match {
            case _: SharedCacheManagerReference => cacheNOL.injectRequest(linkerBundle)
            case _: TrafficReference            => trafficNOL.injectRequest(linkerBundle)
            case _: SystemObjectReference       => SystemObjectPresenceHandler.injectRequest(linkerBundle)
            case _                              =>
                otherLinkers.get(reference.getClass) match {
                    case Some(linker: TrafficInterestedNPH) => linker.getPresence(reference)
                    case None                               => defaultNOL.injectRequest(linkerBundle)
                }
        }
    }
    
    private def startObjectManagement(): Unit = omc.addRequestListener(handleRequest)
    

    object SystemObjectPresenceHandler extends AbstractNetworkPresenceHandler[SystemObjectReference](null, omc) {
        
        def findObject(location: SystemObjectReference): Option[NetworkObject[_ <: SystemObjectReference]] = {
            location match {
                case NetworkConnectionReference => Some(connection)
                case NetworkReference           => Some(network)
                case ApplicationReference       => Some(application)
                case er: EngineReference        => network.findEngine(er.identifier)
                case TrafficReference           => Some(connection.traffic.asInstanceOf[NetworkObject[_ <: SystemObjectReference]]) //TODO remove
                case _                          => None
            }
        }
        
    }
    
}

object GeneralNetworkObjectLinkerImpl {
    
    final val ReferenceAttributeKey: String = "ref"
}
