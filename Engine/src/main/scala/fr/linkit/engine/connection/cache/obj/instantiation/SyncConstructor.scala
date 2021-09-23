package fr.linkit.engine.connection.cache.obj.instantiation

import java.lang.reflect.{Constructor, Modifier}

import fr.linkit.api.connection.cache.obj.SynchronizedObject
import fr.linkit.api.connection.cache.obj.instantiation.SyncInstanceGetter
import fr.linkit.engine.connection.packet.persistence.context.structure.ArrayObjectStructure

import scala.reflect.{ClassTag, classTag}

class SyncConstructor[T](constructor: Constructor[T], arguments: Array[Any]) extends SyncInstanceGetter[T] {
    override val tpeClass: Class[_] = constructor.getDeclaringClass

    override def getInstance(syncClass: Class[T with SynchronizedObject[T]]): T with SynchronizedObject[T] = {
        syncClass.getDeclaredConstructor(constructor.getParameterTypes: _*)
            .newInstance(arguments: _*)
    }
}

object SyncConstructor {


    def apply[T: ClassTag](params: Any*): SyncConstructor[T] = {
        val clazz        = classTag[T].runtimeClass
        val objectsArray = params.toArray
        for (constructor <- clazz.getDeclaredConstructors) {
            val params               = constructor.getParameterTypes
            val constructorStructure = ArrayObjectStructure(params)
            if (constructorStructure.isAssignable(objectsArray)) {
                val mods = constructor.getModifiers
                if (Modifier.isPrivate(mods) || Modifier.isProtected(mods))
                    throw new IllegalArgumentException("Provided method objects structure matches a non public constructor")
                return new SyncConstructor[T](constructor.asInstanceOf[Constructor[T]], objectsArray)
            }

        }
        throw new NoSuchElementException(s"Could not find a constructor matching arguments $params")
    }

}
