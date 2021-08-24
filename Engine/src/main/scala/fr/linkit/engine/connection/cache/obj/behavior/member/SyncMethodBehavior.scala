package fr.linkit.engine.connection.cache.obj.behavior.member

import fr.linkit.api.connection.cache.obj.behavior.member.method.InternalMethodBehavior
import fr.linkit.api.connection.cache.obj.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.connection.cache.obj.behavior.{RMIRulesAgreement, RMIRulesAgreementBuilder, RemoteInvocationRule}
import fr.linkit.api.connection.cache.obj.description.MethodDescription
import fr.linkit.api.connection.cache.obj.invokation.local.LocalMethodInvocation
import fr.linkit.api.connection.cache.obj.invokation.remote.{MethodInvocationHandler, Puppeteer}
import fr.linkit.api.connection.network.Network
import fr.linkit.api.local.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

case class SyncMethodBehavior(override val desc: MethodDescription,
                              override val parameterBehaviors: Array[ParameterBehavior[Any]],
                              override val syncReturnValue: Boolean,
                              override val isHidden: Boolean,
                              private val rules: Array[RemoteInvocationRule],
                              @Nullable override val procrastinator: Procrastinator,
                              @Nullable override val handler: MethodInvocationHandler) extends InternalMethodBehavior {

    override def getName: String = desc.method.getName

    /**
     * @return true if the method can perform an RMI
     * */
    val isRMIEnabled: Boolean = {
        handler != null
    }

    override val isActivated: Boolean = isRMIEnabled

    private val usesRemoteParametersModifier: Boolean = parameterBehaviors.exists(_ != null)

    /**
     * The default return value of the method, based on
     * [[https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html the default primitives value]].
     * */
    override lazy val defaultReturnValue: Any = {
        val clazz = desc.method.getReturnType
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

    override def completeAgreement(agreement: RMIRulesAgreementBuilder): RMIRulesAgreement = {
        rules.foreach(_.apply(agreement))
        agreement.result
    }

    /**
     * uses the given RMIDispatcher
     *
     * @param dispatcher the dispatcher to use
     */
    override def dispatch(dispatcher: Puppeteer[AnyRef]#RMIDispatcher, network: Network, invocation: LocalMethodInvocation[_]): Unit = {
        val args = invocation.methodArguments
        if (!usesRemoteParametersModifier) {
            dispatcher.broadcast(args)
            return
        }
        val buff = args.clone()
        dispatcher.foreachEngines(engineID => {
            for (i <- args.indices) {
                val paramBehavior = parameterBehaviors(i)
                val modifier = paramBehavior.modifier
                buff(i) = modifier.forRemote(args(i), invocation, network.findEngine(engineID).get)
            }
            buff
        })
    }

}
