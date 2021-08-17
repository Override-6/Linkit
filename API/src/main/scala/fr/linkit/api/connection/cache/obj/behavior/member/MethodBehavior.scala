package fr.linkit.api.connection.cache.obj.behavior.member

import fr.linkit.api.connection.cache.obj.behavior.{RMIRulesAgreement, RMIRulesAgreementBuilder}
import fr.linkit.api.connection.cache.obj.description.MethodDescription
import fr.linkit.api.connection.cache.obj.invokation.remote.{Puppeteer, RemoteMethodInvocationHandler}
import fr.linkit.api.local.concurrency.Procrastinator
import org.jetbrains.annotations.Nullable


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

    val usesRemoteParametersModifier: Boolean = parameterBehaviors.exists(_.remoteParamModifier != null)

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

    /**
     * uses the given RMIDispatcher
     *
     * @param dispatcher the dispatcher to use
     * @param args       the arguments that will be used for the RMI request. <br>*
     */
    def dispatch(dispatcher: Puppeteer[AnyRef]#RMIDispatcher, args: Array[Any]): Unit = {
        if (!usesRemoteParametersModifier) {
            dispatcher.broadcast(args)
            return
        }
        val buff = args.clone()
        dispatcher.foreachEngines(engineID => {
            for (i <- args.indices) {
                val paramBehavior  = parameterBehaviors(i)
                val remoteModifier = paramBehavior.remoteParamModifier
                if (remoteModifier != null)
                    buff(i) = remoteModifier.forRemote(args(i), engineID)
            }
            buff
        })
    }

}
