package fr.linkit.engine.internal.language.bhv

import fr.linkit.api.gnom.network.Network
import fr.linkit.api.internal.concurrency.Procrastinator

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
        case obj: ObjectsProperty => obj.name == name && obj.map == map && obj.parent == parent
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
            //"server" -> network.serverEngine,
            "traffic" -> network.connection.traffic,
            ))
    }
    
}