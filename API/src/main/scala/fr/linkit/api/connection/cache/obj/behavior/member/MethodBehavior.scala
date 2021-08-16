package fr.linkit.api.connection.cache.obj.behavior.member

import fr.linkit.api.connection.cache.obj.behavior.{RMIRulesAgreement, RMIRulesAgreementBuilder}
import fr.linkit.api.connection.cache.obj.description.MethodDescription
import fr.linkit.api.connection.cache.obj.invokation.remote.RemoteMethodInvocationHandler
import fr.linkit.api.local.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

/**
 * Determines the behaviors of a method invocation (local or remote).
 *
 * @param desc               the method description
 * @param synchronizedParams a Seq of boolean representing the method's arguments where the arguments at index n is synchronized if synchronizedParams(n) == true.
 * @param syncReturnValue    if true, synchronize the return value of the method.
 * @param isHidden           If hidden, this method should not be called from distant engines.
 * @param invocationRules    //TODO doc
 * @param procrastinator     if not null, the procrastinator that will process the method call
 * @param handler            the used [[RemoteMethodInvocationHandler]]
 */
case class MethodBehavior(desc: MethodDescription,
                          parameterBehaviors: Array[MethodParameterBehavior[Any]],
                          syncReturnValue: Boolean,
                          isHidden: Boolean,
                          private val rules: Array[RemoteInvocationRule],
                          @Nullable procrastinator: Procrastinator,
                          @Nullable handler: RemoteMethodInvocationHandler) {

    /**
     * @return true if the method can perform an RMI
     * */
    val isRMIEnabled: Boolean = {
        handler != null
    }

    /**
     * The default return value of the method, based on
     * [[https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html the default primitives value]].
     * */
    lazy val defaultReturnValue: Any = {
        val clazz = desc.javaMethod.getReturnType
        if (clazz.isPrimitive) {
            import java.{lang => l}
            clazz match {
                case l.Character.TYPE => '\u0000'
                case l.Boolean.TYPE   => false
                case _                => 0
            }
        } else {
            null
        }
    }

    def completeAgreement(agreement: RMIRulesAgreementBuilder): RMIRulesAgreement = {
        rules.foreach(_.apply(agreement))
        agreement.result
    }

}
