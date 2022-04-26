package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller
import fr.linkit.api.gnom.network.statics.StaticsCaller
import fr.linkit.engine.gnom.cache.sync.instantiation.Constructor.getAssignableConstructor

class SyncStaticAccessInstanceCreator(override val tpeClass: Class[StaticsCaller],
                                      arguments: Array[Any],
                                      val targettedClass: Class[_]) extends SyncInstanceCreator[StaticsCaller] {

    override def getInstance(syncClass: Class[StaticsCaller with SynchronizedObject[StaticsCaller]]): StaticsCaller with SynchronizedObject[StaticsCaller] = {
        val constructor = getAssignableConstructor(syncClass, arguments)
        constructor.newInstance(arguments: _*)
    }

    override def getOrigin: Option[StaticsCaller] = None
}