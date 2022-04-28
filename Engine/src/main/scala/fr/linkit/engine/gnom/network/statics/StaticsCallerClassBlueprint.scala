package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.internal.generation.compilation.access.CompilerType
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncStaticsDescription
import fr.linkit.engine.gnom.cache.sync.generation.sync.ScalaBlueprintUtilities.{getParameters, toScalaString}
import fr.linkit.engine.gnom.cache.sync.generation.sync.{CastsScope, SyncMethodScope}
import fr.linkit.engine.internal.generation.compilation.access.CommonCompilerType
import fr.linkit.engine.internal.language.cbp.AbstractClassBlueprint

import java.io.InputStream
import java.lang.reflect.Method

class StaticsCallerClassBlueprint(bp: InputStream) extends AbstractClassBlueprint[SyncStaticsDescription[_]](bp) {

    override val compilerType: CompilerType   = CommonCompilerType.Scalac
    override val rootScope   : RootValueScope = new RootValueScope {
        bindValue("OriginClassSimpleName" ~> (_.clazz.getSimpleName))
        bindValue("OriginClassName" ~> (_.clazz.getTypeName))
        bindValue("ClassName" ~> (_.clazz.getSimpleName + "StaticsCaller"))

        bindSubScope("INHERITED_METHODS", new SyncMethodScope(_, _, _) {
            bindValue("OriginClassSimpleName" ~> (_.classDesc.clazz.getSimpleName))
            bindValue("MethodNameID" ~> getMethodNameID)
            bindValue("ParamsOutMatch" ~> getParamsOutMatch)
            bindValue("ParamsOut" ~> (getParameters(_, false, true)))
        }, (c, f: MethodDescription => Unit) => c.listMethods().foreach(f))
        bindSubScope("CASTS", new CastsScope(_, _, _), (desc, action: Int => Unit) => {
            val casts = desc.listMethods()
                    .toSeq
                    .flatMap(x => countCompsTParams(x.javaMethod))
                    .distinct
                    .filter(_ > 0)
            casts.foreach(action)
        })
    }
    private def countCompsTParams(method: Method): Seq[Int] = {
        def countTParams(clazz: Class[_]): Int = clazz.getTypeParameters.size

        Seq[Int](countTParams(method.getReturnType)) ++ method.getParameterTypes.map(countTParams)
    }

    private def getMethodNameID(desc: MethodDescription): String = {
        val methodID = desc.methodId
        if (methodID < 0) s"_${methodID.abs}" else methodID.toString
    }

    private def getParamsOutMatch(desc: MethodDescription): String = {
        desc.javaMethod
                .getParameterTypes
                .zipWithIndex.map { case (c, i) =>
            s"args($i).asInstanceOf[${toScalaString(c)}]"
        }.mkString(",")
    }
}
