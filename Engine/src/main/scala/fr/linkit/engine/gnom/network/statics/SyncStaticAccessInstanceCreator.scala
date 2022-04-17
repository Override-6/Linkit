package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller
import fr.linkit.engine.gnom.cache.sync.instantiation.Constructor.getAssignableConstructor

class SyncStaticAccessInstanceCreator(override val tpeClass: Class[MethodCaller],
                                      arguments: Array[Any],
                                      val staticsClass: Class[_]) extends SyncInstanceCreator[MethodCaller] {

    override def getInstance(syncClass: Class[MethodCaller with SynchronizedObject[MethodCaller]]): MethodCaller with SynchronizedObject[MethodCaller] = {
        val constructor = getAssignableConstructor(syncClass, arguments)
        constructor.newInstance(arguments: _*)
    }

    override def getOrigin: Option[AnyRef] = None
}