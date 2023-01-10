/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package linkit.base.network.statics

import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import fr.linkit.api.internal.compilation.access.CompilerType
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncStaticsDescription
import fr.linkit.engine.gnom.cache.sync.generation.ScalaBlueprintUtilities.{getParameters, toScalaString}
import fr.linkit.engine.gnom.cache.sync.generation.{CastsScope, SyncMethodScope}
import fr.linkit.engine.internal.compilation.access.CommonCompilerType
import fr.linkit.engine.internal.compilation.cbp.AbstractClassBlueprint

import java.io.InputStream
import java.lang.reflect.Method

class StaticsCallerClassBlueprint(bp: InputStream) extends AbstractClassBlueprint[SyncStaticsDescription[_]](bp) {

    override val compilerType: CompilerType   = CommonCompilerType.Scalac
    override val rootScope   : RootValueScope = new RootValueScope {
        bindValue("OriginClassSimpleName" ~> (_.specs.mainClass.getSimpleName))
        bindValue("OriginClassName" ~> (_.specs.mainClass.getTypeName))
        bindValue("ClassName" ~> (_.specs.mainClass.getSimpleName + "StaticsCaller"))

        bindSubScope("INHERITED_METHODS", new SyncMethodScope(_, _, _) {
            bindValue("OriginClassSimpleName" ~> (_.classDesc.specs.mainClass.getSimpleName))
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
