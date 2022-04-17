package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.internal.generation.compilation.access.CompilerType
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncStaticsDescription
import fr.linkit.engine.gnom.cache.sync.generation.sync.ScalaBlueprintUtilities.{getParameters, toScalaString}
import fr.linkit.engine.gnom.cache.sync.generation.sync.SyncMethodBlueprint
import fr.linkit.engine.internal.generation.compilation.access.CommonCompilerType
import fr.linkit.engine.internal.language.cbp.AbstractClassBlueprint

import java.io.InputStream

class StaticsCallerClassBlueprint(bp: InputStream) extends AbstractClassBlueprint[SyncStaticsDescription[_]](bp) {

    override val compilerType: CompilerType   = CommonCompilerType.Scalac
    override val rootScope   : RootValueScope = new RootValueScope {
        bindValue("OriginClassSimpleName" ~> (_.clazz.getSimpleName))
        bindValue("OriginClassName" ~> (_.clazz.getTypeName))
        bindValue("ClassName" ~> (_.clazz.getSimpleName + "StaticsCaller"))

        bindSubScope(new SyncMethodBlueprint.ValueScope("INHERITED_METHODS", _, _) {
            bindValue("MethodNameID" ~> getMethodNameID)
            bindValue("ParamsOutMatch" ~> getParamsOutMatch)
            bindValue("ParamsOut" ~> (getParameters(_, false)))
        }, (c, f: MethodDescription => Unit) => c.listMethods().foreach(f))
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
