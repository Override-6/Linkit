package fr.linkit.engine.internal.language.bhv

import fr.linkit.api.gnom.network.Network
import fr.linkit.api.internal.concurrency.Procrastinator

class ObjectsProperty private(parent: PropertyClass,
                              private val map: Map[String, AnyRef]) extends PropertyClass {
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

    def +(other: ObjectsProperty): ObjectsProperty = new ObjectsProperty(this, other.map)

}

object ObjectsProperty {
    def apply(map: Map[String, AnyRef]): ObjectsProperty = {
        new ObjectsProperty(null, map)
    }

    implicit def empty: ObjectsProperty = apply(Map())

    def defaults(network: Network): ObjectsProperty = {
        apply(Map(
            "network" -> network,
            "connection" -> network.connection,
            "app" -> network.connection.getApp,
            "application" -> network.connection.getApp,
            //"server" -> network.serverEngine,
            "gnol" -> network.gnol,
            "traffic" -> network.connection.traffic,
        ))
    }
}