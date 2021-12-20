package fr.linkit.engine.gnom.cache.sync.contract.description

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.{BasicInvocationRule, FullRemote}
import fr.linkit.api.gnom.persistence.context.{Deconstructible, Persist}
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription.SyntheticMod

import java.lang.reflect.{Executable, Modifier}
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

class SyncObjectDescription[A <: AnyRef] @Persist() private(clazz: Class[A]) extends AbstractSyncStructureDescription[A](clazz) with Deconstructible {

    val (nonOriginalObjectsImplClass, fullRemoteDefaultRule) = computeImplementationLevel(clazz)

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

    override def deconstruct(): Array[Any] = Array(clazz)

    private def isNotOverridable(e: Executable): Boolean = {
        val mods = e.getModifiers
        import Modifier._
        isPrivate(mods) || isStatic(mods) || isFinal(mods) || isNative(mods) || (mods & SyntheticMod) != 0
    }

    private def computeImplementationLevel(clazz: Class[_]): (Class[_], Option[BasicInvocationRule]) = {
        var cl: Class[_] = clazz
        while (cl != null) {
            if (cl.isAnnotationPresent(classOf[FullRemote]))
                return (cl, Some(cl.getAnnotation(classOf[FullRemote]).value()))
            cl = cl.getSuperclass
        }
        val interfaces = clazz.getInterfaces
        if (interfaces.nonEmpty) {
            for (itf <- interfaces) {
                val (clazz, rule) = computeImplementationLevel(itf)
                if (clazz != itf || rule.nonEmpty)
                    return (clazz, rule)
            }
        }
        (clazz, None)
    }
}

object SyncObjectDescription {

    private val SyntheticMod = 0x00001000

    private val cache = mutable.HashMap.empty[Class[_], SyncObjectDescription[_]]

    implicit def fromTag[A <: AnyRef : ClassTag]: SyncObjectDescription[A] = apply[A](classTag[A].runtimeClass)

    def apply[A <: AnyRef](clazz: Class[_]): SyncObjectDescription[A] = cache.getOrElseUpdate(clazz, {
        if (classOf[SynchronizedObject[_]].isAssignableFrom(clazz))
            throw new IllegalArgumentException("Provided class already extends from SynchronizedObject")
        val AClass = clazz.asInstanceOf[Class[A]]
        new SyncObjectDescription[A](AClass)
    }).asInstanceOf[SyncObjectDescription[A]]

}