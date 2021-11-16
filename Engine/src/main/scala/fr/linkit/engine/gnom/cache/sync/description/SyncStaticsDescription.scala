package fr.linkit.engine.gnom.cache.sync.description

import java.lang.reflect.{Executable, Modifier}

import fr.linkit.api.gnom.cache.sync.SynchronizedObject

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class SyncStaticsDescription[A <: AnyRef](clazz: Class[A]) extends AbstractSyncStructureDescription[A](clazz) {
    override protected def applyNotFilter(e: Executable): Boolean = {
        val mods = e.getModifiers
        !Modifier.isStatic(mods) || e.isSynthetic
    }
}

object SyncStaticsDescription {
    private val cache = mutable.HashMap.empty[Class[_], SyncStaticsDescription[_]]

    implicit def fromTag[A: ClassTag]: SyncStaticsDescription[A] = apply[A](classTag[A].runtimeClass)

    def apply[A](clazz: Class[_]): SyncStaticsDescription[A] = cache.getOrElseUpdate(clazz, {
        if (classOf[SynchronizedObject[_]].isAssignableFrom(clazz))
            throw new IllegalArgumentException("Provided class already extends from SynchronizedObject")
        val AClass = clazz.asInstanceOf[Class[A]]
        new SyncStaticsDescription[A](AClass)
    }).asInstanceOf[SyncStaticsDescription[A]]

}
