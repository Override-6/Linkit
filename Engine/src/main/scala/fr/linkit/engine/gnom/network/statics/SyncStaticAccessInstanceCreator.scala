package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.instantiation.SyncInstanceCreator
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller

class SyncStaticAccessInstanceCreator(override val tpeClass: Class[MethodCaller], val staticsClass: Class[_]) extends SyncInstanceCreator[MethodCaller] extends Constructor{

}
