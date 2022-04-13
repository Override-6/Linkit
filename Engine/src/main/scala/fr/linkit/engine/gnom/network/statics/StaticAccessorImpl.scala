package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller
import fr.linkit.api.gnom.network.statics.StaticAccessor
import fr.linkit.engine.gnom.persistence.config.structure.ArrayObjectStructure

import java.lang.reflect.Modifier
import scala.language.dynamics
import scala.reflect.{ClassTag, classTag}

class StaticAccessorImpl(staticCaller: MethodCaller, staticClass: Class[_]) extends StaticAccessor {

    private val staticMethods = {
        staticClass
            .getDeclaredMethods
            .filter(m => Modifier.isStatic(m.getModifiers))
            .tapEach(_.setAccessible(true))
    }

    override def applyDynamic[T: ClassTag](name: String)(params: Any*): T = {
        val returnType  = classTag[T].runtimeClass
        val arrayParams = params.toArray
        val methodName  = getMethodName(name, returnType, arrayParams)
        staticCaller.call(methodName, arrayParams).asInstanceOf[T]
    }

    private def getMethodName(name: String, returnType: Class[_], params: Array[Any]): String = {
        val method = staticMethods.find(m => m.getName == name && m.getReturnType == returnType &&
            ArrayObjectStructure(m.getParameterTypes).isAssignable(params))
            .getOrElse(throw new NoSuchElementException(s"Could not find static method '$name(${params.mkString("Array(", ", ", ")")}): ${returnType.getName}' in class $staticClass"))
        val methodID = MethodDescription.computeID(method)
        if (methodID < 0) s"_${methodID.abs}" else methodID.toString
    }


}
