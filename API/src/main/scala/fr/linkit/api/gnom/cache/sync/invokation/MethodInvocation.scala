package fr.linkit.api.gnom.cache.sync.invokation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.behavior.member.method.MethodBehavior

/**
 * The invocation information for a synchronized object's method.
 *
 * @tparam R the return type of the method invoked
 * */
trait MethodInvocation[R] {

    /**
     * The synchronized object on which the method is called.
     * */
    val synchronizedObject: SynchronizedObject[_]

    /**
     * The method's identifier.
     * */
    val methodID: Int

    /**
     * The method's behavior
     */
    val methodBehavior: MethodBehavior

    val debug: Boolean = true

}
