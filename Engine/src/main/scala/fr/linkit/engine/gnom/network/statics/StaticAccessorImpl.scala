package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller
import fr.linkit.api.gnom.network.statics.StaticAccessor
import fr.linkit.engine.internal.utils.{ScalaUtils, UnWrapper}

import java.lang.reflect.{Method, Modifier}
import scala.language.dynamics
import scala.reflect.{ClassTag, classTag}

class StaticAccessorImpl(staticCaller: MethodCaller, staticClass: Class[_]) extends StaticAccessor {

    private val staticMethods = {
        staticClass
                .getDeclaredMethods
                .filter(m => Modifier.isStatic(m.getModifiers) && Modifier.isPublic(m.getModifiers))
                .filter(ScalaUtils.setAccessible)
    }

    override def applyDynamic[T: ClassTag](name: String)(params: Any*): T = {
        val returnType                    = classTag[T].runtimeClass
        val arrayParams                   = params.toArray
        val (methodName, rectifiedParams) = searchMethod(name, returnType, arrayParams)
        staticCaller.call(methodName, rectifiedParams).asInstanceOf[T]
    }

    private def searchMethod(name: String, returnType: Class[_], params: Array[Any]): (String, Array[Any]) = {
        val mustSpeculate   = returnType == classOf[Nothing] //return type of the method is not specified.
        val method          = {
            val found = staticMethods.filter(m => m.getName == name && (mustSpeculate || m.getReturnType == returnType) && isAssignable(params, m))
            if (found.length == 1) found.head
            else throw new NoSuchElementException(s"Could not find static method '$name(${params.mkString("Array(", ", ", ")")}): ${if (mustSpeculate) "<?>" else returnType.getName}' in class $staticClass")
        }
        val methodID        = MethodDescription.computeID(method)
        val nameID          = if (methodID < 0) s"_${methodID.abs}" else methodID.toString
        val lastParameter   = method.getParameters.lastOption
        val rectifiedParams = if (lastParameter.exists(_.isVarArgs)) params :+ Array()(ClassTag(lastParameter.get.getType.componentType())) else params
        (nameID, rectifiedParams)
    }

    private def isAssignable(args: Array[Any], method: Method): Boolean = {
        val params        = method.getParameters
        val types         = params.map(_.getType)
        val lastIsVarargs = params.lastOption.exists(_.isVarArgs)
        if (!lastIsVarargs && params.length != types.length)
            return false
        val argsLength = args.length
        var i          = 0
        while (i < argsLength) {
            val value = args(i)
            if (value != null) {
                val tpe = if (lastIsVarargs && i >= types.length) types.last else types(i)
                if (!(tpe.isAssignableFrom(value.getClass) || tpe.isAssignableFrom(UnWrapper.getPrimitiveClass(value))))
                    return false
            }
            i += 1
        }
        true
    }

}
