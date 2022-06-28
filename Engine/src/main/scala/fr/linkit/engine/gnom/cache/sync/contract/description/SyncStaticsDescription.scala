package fr.linkit.engine.gnom.cache.sync.contract.description

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncClassDefUnique}
import fr.linkit.api.gnom.network.statics.StaticsCaller
import fr.linkit.api.gnom.persistence.context.{Deconstructible, Persist}

import java.lang.reflect.{Executable, Modifier}
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class SyncStaticsDescription[A <: AnyRef]@Persist()(clazz: Class[A]) extends AbstractSyncStructureDescription[A](SyncClassDef(clazz)) with Deconstructible {

    override def className: String = clazz.getSimpleName + "StaticsCaller"

    override protected def applyNotFilter(e: Executable): Boolean = {
        val mods = e.getModifiers
        !Modifier.isStatic(mods) || e.isSynthetic || !Modifier.isPublic(mods)
    }

    override def deconstruct(): Array[Any] = Array(clazz)
}

object SyncStaticsDescription {

    private val cache = mutable.HashMap.empty[Class[_], SyncStaticsDescription[_]]

    implicit def fromTag[A <: AnyRef : ClassTag]: SyncStaticsDescription[A] = apply[A](classTag[A].runtimeClass)

    def apply[A <: AnyRef](clazz: Class[_]): SyncStaticsDescription[A] = {
        if (classOf[StaticsCaller].isAssignableFrom(clazz))
            return apply(StaticsCaller.getStaticsTarget(clazz.asSubclass(classOf[StaticsCaller])))
        cache.getOrElseUpdate(clazz, {
            if (classOf[SynchronizedObject[_]].isAssignableFrom(clazz))
                throw new IllegalArgumentException("Provided class already extends from SynchronizedObject")
            val AClass = clazz.asInstanceOf[Class[A]]
            new SyncStaticsDescription[A](AClass)
        }).asInstanceOf[SyncStaticsDescription[A]]
    }

}
