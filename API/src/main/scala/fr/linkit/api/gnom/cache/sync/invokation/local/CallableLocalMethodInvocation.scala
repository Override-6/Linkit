package fr.linkit.api.gnom.cache.sync.invokation.local
import fr.linkit.api.gnom.cache.sync.behavior.member.method.{InternalMethodBehavior, MethodBehavior}

trait CallableLocalMethodInvocation[R] extends LocalMethodInvocation[R] {

    override val methodBehavior: InternalMethodBehavior

    /**
     * Calls the local method of the object.
     * @return the method's call result.
     * */
    def callSuper(): R

}
