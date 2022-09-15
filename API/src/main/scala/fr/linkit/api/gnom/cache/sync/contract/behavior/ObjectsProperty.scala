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

package fr.linkit.api.gnom.cache.sync.contract.behavior

import fr.linkit.api.gnom.network.Network
import fr.linkit.api.internal.concurrency.Procrastinator

/**
 * A default implementation of BHVProperties
 * */
class ObjectsProperty private(override val name: String,
                              private val parent: BHVProperties,
                              private val map: Map[String, AnyRef]) extends BHVProperties {
    
    override def get(refName: String): AnyRef = {
        map.get(refName) match {
            case Some(value)            => value
            case None if parent != null => parent.get(refName)
            case None                   => throw new NoSuchElementException(s"Unknown object $refName.")
        }
    }
    
    override def getProcrastinator(refName: String): Procrastinator = get(refName) match {
        case p: Procrastinator => p
        case o                 =>
            throw new ClassCastException(s"Object $refName is not a procrastinator. (${o.getClass.getName} cannot be cast to ${classOf[Procrastinator].getName}")
    }
    
    override def equals(obj: Any): Boolean = obj match {
        case obj: ObjectsProperty => obj.name == name && obj.map.keys == map.keys && obj.parent == parent
        case _                    => false
    }
    
    def +(other: ObjectsProperty): ObjectsProperty = {
        val newName = if (other.name < name) other.name + "+" + name else name + "+" + other.name
        new ObjectsProperty(newName, this, other.map)
    }
    
}

object ObjectsProperty {
    
    def apply(name: String)(map: Map[String, AnyRef]): ObjectsProperty = {
        new ObjectsProperty(name, null, map)
    }
    
    implicit def empty: ObjectsProperty = apply("empty")(Map())
    
    def empty(name: String): ObjectsProperty = apply(name)(Map())
    
    def defaults(network: Network): ObjectsProperty = {
        apply(s"defaults_${network.serverIdentifier}")(Map(
            "network" -> network,
            "connection" -> network.connection,
            "app" -> network.connection.getApp,
            "application" -> network.connection.getApp,
            "traffic" -> network.connection.traffic,
            ))
    }
    
}