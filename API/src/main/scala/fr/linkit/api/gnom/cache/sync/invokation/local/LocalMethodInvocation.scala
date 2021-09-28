package fr.linkit.api.gnom.cache.sync.invokation.local

import fr.linkit.api.gnom.cache.sync.invokation.MethodInvocation

trait LocalMethodInvocation[R] extends MethodInvocation[R] {


    /**
     * The final argument array for the method invocation.
     * */
    val methodArguments: Array[Any]

}
