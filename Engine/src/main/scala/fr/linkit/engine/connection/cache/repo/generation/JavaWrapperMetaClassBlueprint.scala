/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.cache.repo.generation

import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.connection.cache.repo.description.PuppetDescription.MethodDescription
import fr.linkit.api.local.generation.compilation.access.CompilerType
import fr.linkit.engine.connection.cache.repo.generation.JavaWrapperMetaClassBlueprint.{MethodValueScope, getTParams}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.generation.cbp.{AbstractClassBlueprint, AbstractValueScope}
import fr.linkit.engine.local.generation.compilation.access.CommonCompilerTypes

import java.lang.reflect.{GenericDeclaration, Type, TypeVariable}

class JavaWrapperMetaClassBlueprint extends AbstractClassBlueprint[PuppetDescription[_]](classOf[LinkitApplication].getResourceAsStream("/generation/puppet_wrapper_meta_blueprint.jcbp")) {

    override val compilerType: CompilerType   = CommonCompilerTypes.Javac
    override val rootScope   : RootValueScope = new RootValueScope {
        bindValue("WrappedClassPackage" ~> (_.clazz.getPackageName))
        bindValue("WrappedClassSimpleName" ~> (_.clazz.getSimpleName))
        bindValue("WrappedClassName" ~> (_.clazz.getTypeName.replaceAll("\\$", ".")))
        bindValue("TParamsIn" ~> (s => getTParams(s.clazz, _.toString)))
        bindValue("TParamsOut" ~> (s => getTParams(s.clazz, _.getTypeName)))
        bindSubScope(MethodValueScope, (desc, action: MethodDescription => Unit) => {
            desc.listMethods()
                    .distinctBy(_.methodId)
                    .filter(m => m.symbol.isSetter || m.symbol.isGetter)
                    .foreach(action)
        })
    }

}

object JavaWrapperMetaClassBlueprint {

    case class MethodValueScope(blueprint: String, pos: Int)
            extends AbstractValueScope[MethodDescription]("INHERITED_META", pos, blueprint) {

        bindValue("GenericTypes" ~> (m => getTParams(m.method, _.toString)))
        bindValue("ReturnType" ~> (_.method.getGenericReturnType.toString))
        bindValue("MethodName" ~> (_.method.getName))
        bindValue("ParamsIn" ~> (getParameters(_, pair => s"${pair._1} arg${pair._2}")))
        bindValue("ParamsOut" ~> (getParameters(_, pair => s"arg${pair._2}")))
        bindValue("MethodID" ~> (_.methodId.toString))
        bindValue("DefaultReturnType" ~> (_.getDefaultTypeReturnValue))
    }

    private def getParameters(desc: MethodDescription, transform: ((Type, Int)) => String): String = {
        desc.method
                .getGenericParameterTypes
                .zipWithIndex
                .map(transform)
                .mkString("(", ", ", ")")
    }

    private def getTParams(dec: GenericDeclaration, transform: TypeVariable[_] => String): String = {
        val params = dec.getTypeParameters
        if (params.isEmpty) ""
        else params.map(transform).mkString("<", ",", ">")
    }
}
