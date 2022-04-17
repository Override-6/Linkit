package fr.linkit.api.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.SynchronizedObjectCache
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller

trait SynchronizedStaticsCache extends SynchronizedObjectCache[MethodCaller]