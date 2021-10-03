package fr.linkit.engine.gnom.cache.sync.behavior.member

import fr.linkit.api.gnom.cache.sync.behavior.member.method.InternalMethodBehavior
import fr.linkit.api.gnom.cache.sync.behavior.member.method.parameter.ParameterBehavior
import fr.linkit.api.gnom.cache.sync.behavior.{RMIRulesAgreement, RMIRulesAgreementBuilder, RemoteInvocationRule, ObjectBehaviorStore}
import fr.linkit.api.gnom.cache.sync.description.MethodDescription
import fr.linkit.api.gnom.cache.sync.invokation.local.LocalMethodInvocation
import fr.linkit.api.gnom.cache.sync.invokation.remote.{MethodInvocationHandler, Puppeteer}
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.internal.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable

case class SyncMethodBehavior(override val desc: MethodDescription,
                              override val parameterBehaviors: Array[ParameterBehavior[AnyRef]],
                              override val syncReturnValue: Boolean,
                              override val isHidden: Boolean,
                              override val innerInvocations: Boolean,
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
    override def dispatch(dispatcher: Puppeteer[AnyRef]#RMIDispatcher, network: Network, store: ObjectBehaviorStore, invocation: LocalMethodInvocation[_]): Unit = {
        val args = invocation.methodArguments
        if (!usesRemoteParametersModifier) {
            dispatcher.broadcast(args)
            return
        }
        val buff = args.clone()
        dispatcher.foreachEngines(engineID => {
            val engine = network.findEngine(engineID).get
            for (i <- args.indices) {
                val paramBehavior = parameterBehaviors(i)
                val modifier      = paramBehavior.modifier
                buff(i) = store.modifyParameterForRemote(invocation, engine, buff(i).asInstanceOf[AnyRef], paramBehavior)
            }
            buff
        })
    }

}
