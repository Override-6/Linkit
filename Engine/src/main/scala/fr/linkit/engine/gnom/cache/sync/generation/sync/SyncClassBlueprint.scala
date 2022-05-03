/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache.sync.generation.sync

import fr.linkit.api.gnom.cache.sync.contract.description.{MethodDescription, SyncClassDefMultiple, SyncStructureDescription}
import fr.linkit.api.internal.generation.compilation.access.CompilerType
import ScalaBlueprintUtilities._
import fr.linkit.engine.internal.generation.compilation.access.CommonCompilerType
import fr.linkit.engine.internal.language.cbp.AbstractClassBlueprint

import java.io.InputStream
import java.lang.reflect.{Method, TypeVariable}

class SyncClassBlueprint(in: InputStream) extends AbstractClassBlueprint[SyncStructureDescription[_]](in) {

    override val compilerType: CompilerType = CommonCompilerType.Scalac

    override val rootScope: RootValueScope = new RootValueScope {
        bindValue("OriginClassSimpleName" ~> (_.specs.mainClass.getSimpleName))
        bindValue("OriginClassName" ~> (_.specs.mainClass.getSimpleName))
        bindValue("TParamsIn" ~> (getGenericParams(_, typeToScalaDeclaration)))
        bindValue("TParamsOut" ~> (getGenericParams(_, _.getName)))
        bindValue("TParamsInBusted" ~> (getGenericParams(_, _ => "_")))

        bindSubScope("INHERITED_METHODS", new SyncMethodScope(_, _, _), (desc, action: MethodDescription => Unit) => {
            desc.listMethods()
                    .toSeq
                    .distinctBy(x => (x.javaMethod.getParameterTypes.toList, x.getName))
                    .foreach(action)
        })

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

    private def typeToScalaDeclaration(tpe: TypeVariable[_]): String = {
        tpe.getName + " <: " + tpe.getBounds.map(_.getTypeName).mkString(" with ")
    }

}
