package fr.linkit.engine.internal.language.bhv

import fr.linkit.api.gnom.network.Network
import fr.linkit.api.internal.concurrency.Procrastinator

class ObjectsProperty(parent: PropertyClass, map: Map[String, AnyRef]) extends PropertyClass {
    override def get(refName: String): AnyRef = {
        map.get(refName.drop(1)) match {
            case Some(value)            => value
            case None if parent != null => parent.get(refName)
            case None                   => throw new NoSuchElementException(s"Unknown object $refName.")
        }
    }

    override def getProcrastinator(refName: String): Procrastinator = get(refName) match {
        case p: Procrastinator => p
        case o                 => throw new ClassCastException(s"Object $refName is not a procrastinator. (${o.getClass.getName} cannot be cast to ${classOf[Procrastinator].getName}")
    }

}

object ObjectsProperty {
    implicit def default(network: Network): ObjectsProperty = {
        val map: Map[String, AnyRef] = Map(
            "network" -> network,
            "connection" -> network.connection,
            "app" -> network.connection.getApp,
            "application" -> network.connection.getApp,
            //"server" -> network.serverEngine,
            "gnol" -> network.gnol,
            "traffic" -> network.connection.traffic,
        )
        new ObjectsProperty(null, map)
    }
}