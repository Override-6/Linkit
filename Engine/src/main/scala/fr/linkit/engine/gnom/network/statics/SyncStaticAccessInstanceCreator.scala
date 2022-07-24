package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncClassDefUnique}
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator
import fr.linkit.api.gnom.network.statics.StaticsCaller
import fr.linkit.engine.gnom.cache.sync.instantiation.New.getAssignableConstructor

class SyncStaticAccessInstanceCreator(tpeClass: Class[StaticsCaller],
                                      arguments: Array[Any],
                                      val targettedClass: Class[_]) extends SyncInstanceCreator[StaticsCaller] {

    override val syncClassDef: SyncClassDef = SyncClassDef(tpeClass)

    override def getInstance(syncClass: Class[StaticsCaller with SynchronizedObject[StaticsCaller]]): StaticsCaller with SynchronizedObject[StaticsCaller] = {
        val constructor = getAssignableConstructor(syncClass, arguments)
        constructor.newInstance(arguments: _*)
    }

    override def getOrigin: Option[StaticsCaller] = None
}