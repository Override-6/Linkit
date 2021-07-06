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

package fr.linkit.engine.connection.cache.repo.generation.bp

import fr.linkit.api.connection.cache.repo.description.PuppetDescription.MethodDescription
import fr.linkit.api.local.generation.compilation.access.CompilerType
import fr.linkit.engine.local.generation.cbp.AbstractClassBlueprint
import fr.linkit.engine.local.generation.compilation.access.CommonCompilerTypes

import java.io.InputStream

class JavaWrapperAnonFunMethodBlueprint(bp: InputStream) extends AbstractClassBlueprint[MethodDescription](bp) {

    override val compilerType: CompilerType   = CommonCompilerTypes.Javac
    override val rootScope   : RootValueScope = new RootValueScope {
        bindValue("ReturnType" ~> (_.method.getReturnType.getTypeName))
        bindValue("MethodName" ~> (_.method.getName))
        bindValue("GenericTypesOut" ~> getGenericTypesOut)
        bindValue("ParamsOut" ~> getParamsOut)
    }

    private def getGenericTypesOut(desc: MethodDescription): String = {
        val str = desc.method
                .getTypeParameters
                .map(_.getName)
                .mkString(", ")
        if (str.isEmpty)
            str
        else s"<$str>"
    }

    private def getParamsOut(desc: MethodDescription): String = {
        desc.method
                .getParameterTypes
                .zipWithIndex
                .map(p => s"$$${p._2 + 1}")
                .mkString("(", ", ", ")")
    }
}
