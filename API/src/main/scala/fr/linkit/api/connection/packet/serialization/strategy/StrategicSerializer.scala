package fr.linkit.api.connection.packet.serialization.strategy

import fr.linkit.api.connection.packet.serialization.Serializer
import org.jetbrains.annotations.Nullable

import scala.reflect.ClassTag

trait StrategicSerializer extends Serializer with StrategyHolder {

    def deserializeObject[S <: Serializable : ClassTag](array: Array[Byte], @Nullable typeHint: Class[S] = null): S

}
