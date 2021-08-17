package fr.linkit.api.connection.cache.obj.invokation.local

import fr.linkit.api.connection.cache.obj.invokation.MethodInvocation

trait LocalMethodInvocation[R] extends MethodInvocation[R] {


    /**
     * The final argument array for the method invocation.
     * */
    val methodArguments: Array[Any]

    /**
     * Calls the local method of the object.
     * @return the method's call result.
     * */
    def callSuper(): R

}
