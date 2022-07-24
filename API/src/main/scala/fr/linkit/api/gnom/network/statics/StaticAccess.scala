package fr.linkit.api.gnom.network.statics
import scala.reflect.ClassTag

trait StaticAccess {

    def apply[S: ClassTag]: StaticAccessor
}
