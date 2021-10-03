package fr.linkit.engine.gnom.cache.sync.instantiation

import java.lang.reflect.{Constructor, Modifier}

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceGetter
import fr.linkit.engine.gnom.cache.sync.instantiation.SyncConstructor.getAssignableConstructor
import fr.linkit.engine.gnom.persistence.context.structure.ArrayObjectStructure

import scala.reflect.{ClassTag, classTag}

class SyncConstructor[T](clazz: Class[_], arguments: Array[Any]) extends SyncInstanceGetter[T] {
    override val tpeClass: Class[_] = clazz

    override def getInstance(syncClass: Class[T with SynchronizedObject[T]]): T with SynchronizedObject[T] = {
        val constructor = getAssignableConstructor(syncClass, arguments)
        constructor.newInstance(arguments: _*)
    }
}

object SyncConstructor {


    def apply[T: ClassTag](params: Any*): SyncConstructor[T] = {
        val clazz        = classTag[T].runtimeClass
        val objectsArray = params.toArray
        new SyncConstructor[T](clazz, objectsArray)
    }

    def getAssignableConstructor[T](clazz: Class[T], objectsArray: Array[Any]): Constructor[T] = {
        for (constructor <- clazz.getDeclaredConstructors) {
            val params               = constructor.getParameterTypes
            val constructorStructure = ArrayObjectStructure(params)
            if (constructorStructure.isAssignable(objectsArray)) {
                val mods = constructor.getModifiers
                if (Modifier.isPrivate(mods) || Modifier.isProtected(mods))
                    throw new IllegalArgumentException("Provided method objects structure matches a non public constructor")
                return constructor.asInstanceOf[Constructor[T]]
            }
        }
        throw new NoSuchElementException(s"Could not find a constructor matching arguments ${objectsArray.mkString("Array(", ", ", ")")}")
    }

}
