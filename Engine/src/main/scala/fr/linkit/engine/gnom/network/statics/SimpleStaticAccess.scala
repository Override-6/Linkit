package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.SynchronizedObjectCache
import fr.linkit.api.gnom.network.statics.{ClassStaticAccessor, StaticAccess}

import scala.reflect.{ClassTag, classTag}

class SimpleStaticAccess(cache: SynchronizedObjectCache[ClassStaticAccessor[AnyRef]]) extends StaticAccess {
    override def apply[T <: AnyRef : ClassTag]: ClassStaticAccessor[T] = {
        ???
    }
}
