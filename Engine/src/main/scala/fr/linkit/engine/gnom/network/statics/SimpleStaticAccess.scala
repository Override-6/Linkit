package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.SynchronizedObjectCache
import fr.linkit.api.gnom.network.statics.{ClassStaticAccessor, StaticAccess}
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncStaticsDescription
import fr.linkit.engine.gnom.cache.sync.instantiation.Constructor

import scala.reflect.{ClassTag, classTag}

class SimpleStaticAccess(cache: SynchronizedObjectCache[ClassStaticAccessor[_ <: AnyRef]]) extends StaticAccess {
    override def apply[T <: AnyRef : ClassTag]: ClassStaticAccessor[T] = {
        val clazz = classTag[T].runtimeClass
        cache.getOrSynchronize(clazz.getName.hashCode)(Constructor[SimpleClassStaticAccessor[T]](SyncStaticsDescription(clazz)))
            .asInstanceOf[ClassStaticAccessor[T]]
    }
}
