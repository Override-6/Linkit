package fr.linkit.core.connection.packet.serialization.strategies

import fr.linkit.api.connection.packet.serialization.strategy.{SerialStrategy, StrategicSerializer, StrategyHolder}
import fr.linkit.core.connection.packet.serialization.strategies.MapStrategy.cast

import scala.collection.immutable.VectorMap
import scala.collection.{MapFactory, SortedMapFactory, immutable, mutable}
import scala.reflect.{ClassTag, classTag}

class MapStrategy[M <: Map[_, _] : ClassTag] private(factory: MapFactory[Map])
        extends SerialStrategy[M] {

    override def getTypeHandling: Class[M] = cast[Class[M]](classTag[M].runtimeClass)

    override def serial(instance: M, serializer: StrategicSerializer): Array[Byte] = {
        val tuples = instance.toArray
        serializer.serialize(tuples, false)
    }

    override def deserial(bytes: Array[Byte], serializer: StrategicSerializer): Any = {
        val tuples = serializer.deserializeObject[Array[(_, _)]](bytes)
        factory.from(tuples)
    }

}

object MapStrategy {

    type I <: Map[_, _]

    case class SortedMapStrategy[M <: Map[_, _] : ClassTag] private(factory: SortedMapFactory[Map])
            extends SerialStrategy[M] {

        override def getTypeHandling: Class[M] = cast[Class[M]](classTag[M].runtimeClass)

        override def serial(instance: M, serializer: StrategicSerializer): Array[Byte] = {
            val tuples = instance.toArray
            serializer.serialize(tuples, false)
        }

        override def deserial(bytes: Array[Byte], serializer: StrategicSerializer): Any = {
            val tuples = serializer.deserializeObject[Array[(_, _)]](bytes)
            factory.from(tuples)
        }
    }

    def fromFactory[I <: Map[_, _] : ClassTag](mapFactory: MapFactory[Map])
    : MapStrategy[I] = {
        new MapStrategy[I](mapFactory)
    }

    def apply[I <: Map[_, _]](mapFactory: MapFactory[Map])
    : MapStrategy[I] = {
        fromFactory(mapFactory)
    }

    def immutableHashMap: MapStrategy[I] = MapStrategy(immutable.HashMap)

    def mutableHashMap: MapStrategy[I] = MapStrategy(cast[MapFactory[Map]](mutable.HashMap))

    def mutableTreeMap: SortedMapStrategy[I] = SortedMapStrategy(cast[SortedMapFactory[Map]](mutable.TreeMap))

    def immutableTreeMap: SortedMapStrategy[I] = SortedMapStrategy(immutable.TreeMap)

    def linkedHashMap: MapStrategy[I] = MapStrategy(cast[MapFactory[Map]](mutable.LinkedHashMap))

    def vectorMap: MapStrategy[I] = MapStrategy(VectorMap)

    def attachDefaultStrategies(serializer: StrategyHolder): Unit = {
        Array(immutableHashMap, mutableHashMap, mutableTreeMap, immutableTreeMap, linkedHashMap, vectorMap)
                .foreach(serializer.attachStrategy)
    }

    private def cast[S](s: Any): S = {
        val v = s.asInstanceOf[S]
        println(s"v = ${v}")
        v
    }

}
