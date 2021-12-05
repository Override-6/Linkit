package fr.linkit.api.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.contract.behavior.SynchronizedObjectContractFactory

import scala.reflect.ClassTag

trait StaticAccess {

    def apply[T <: AnyRef: ClassTag]: ClassStaticAccessor[T]

}
