package fr.linkit.engine.gnom.cache.sync.description

import java.lang.reflect.{Executable, Modifier}

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.engine.gnom.cache.sync.description.SyncObjectDescription.SyntheticMod

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class SyncObjectDescription[A <: AnyRef] private(clazz: Class[A]) extends AbstractSyncStructureDescription[A](clazz) {
    override protected def applyNotFilter(e: Executable): Boolean = {
        isNotOverridable(e) ||
            //FIXME Weird bug due to scala's Any and AnyRef stuff...
            (e.getName == "equals" && e.getParameterTypes.length == 1) ||

        //FIXME Bug occurred for objects that extends NetworkObject[A].
        // as SynchronizedObject trait also extends NetworkObject[B],
        // a collision may occur as the generated method would be
        // syncClass#reference: A, which overrides SynchronizedObject#reference: B (there is an incompatible type definition)
        // Maybe making the GNOLinkage able to support multiple references to an object would help
        (e.getName == "reference" && e.getParameterTypes.isEmpty)
    }

    private def isNotOverridable(e: Executable): Boolean = {
        val mods = e.getModifiers
        import Modifier._
        isPrivate(mods) || isStatic(mods) || isFinal(mods) || isNative(mods) || (mods & SyntheticMod) != 0
    }
}

object SyncObjectDescription {

    private val SyntheticMod = 0x00001000

    private val cache = mutable.HashMap.empty[Class[_], SyncObjectDescription[_]]

    implicit def fromTag[A: ClassTag]: SyncObjectDescription[A] = apply[A](classTag[A].runtimeClass)

    def apply[A](clazz: Class[_]): SyncObjectDescription[A] = cache.getOrElseUpdate(clazz, {
        if (classOf[SynchronizedObject[_]].isAssignableFrom(clazz))
            throw new IllegalArgumentException("Provided class already extends from SynchronizedObject")
        val AClass = clazz.asInstanceOf[Class[A]]
        new SyncObjectDescription[A](AClass)
    }).asInstanceOf[SyncObjectDescription[A]]

}