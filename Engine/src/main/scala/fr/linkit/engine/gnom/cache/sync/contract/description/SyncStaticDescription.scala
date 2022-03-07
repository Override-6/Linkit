package fr.linkit.engine.gnom.cache.sync.contract.description

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.persistence.context.{Deconstructible, Persist}

import java.lang.reflect.{Executable, Modifier}
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class SyncStaticDescription[A <: AnyRef]@Persist()(clazz: Class[A]) extends AbstractSyncStructureDescription[A](clazz) with Deconstructible {

    override protected def applyNotFilter(e: Executable): Boolean = {
        val mods = e.getModifiers
        !Modifier.isStatic(mods) || e.isSynthetic
    }

    override def deconstruct(): Array[Any] = Array(clazz)
}

object SyncStaticDescription {

    private val cache = mutable.HashMap.empty[Class[_], SyncStaticDescription[_]]

    implicit def fromTag[A <: AnyRef : ClassTag]: SyncStaticDescription[A] = apply[A](classTag[A].runtimeClass)

    def apply[A <: AnyRef](clazz: Class[_]): SyncStaticDescription[A] = cache.getOrElseUpdate(clazz, {
        if (classOf[SynchronizedObject[_]].isAssignableFrom(clazz))
            throw new IllegalArgumentException("Provided class already extends from SynchronizedObject")
        val AClass = clazz.asInstanceOf[Class[A]]
        new SyncStaticDescription[A](AClass)
    }).asInstanceOf[SyncStaticDescription[A]]

}
