package fr.linkit.api.gnom.network.statics

import scala.reflect.ClassTag

trait StaticAccess {

    def apply[T <: AnyRef: ClassTag]: ClassStaticAccessor[T]

}
