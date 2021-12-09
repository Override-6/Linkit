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

package fr.linkit.engine.gnom.cache.sync.generation.reference

import fr.linkit.api.internal.generation.compilation.access.CompilerType
import fr.linkit.engine.gnom.cache.sync.generation.reference.JavaBlueprintUtilities._
import fr.linkit.engine.internal.generation.compilation.access.CommonCompilerTypes
import fr.linkit.engine.internal.language.cbp.AbstractClassBlueprint

import java.io.InputStream
import java.lang.reflect.{Constructor, TypeVariable}

class ExtendedReferenceClassBlueprint(in: InputStream) extends AbstractClassBlueprint[ExtendedReferenceCompilationContext](in) {

    override val compilerType: CompilerType   = CommonCompilerTypes.Javac
    override val rootScope   : RootValueScope = new RootValueScope {
        bindValue("ReferenceClassName" ~> (_.getName))
        bindValue("ReferenceSimpleName" ~> (_.getSimpleName))

        bindValue("ConstructorOut" ~> constructorOut)

        bindValue("TParamsIn" ~> (getGenericParams(_, typeToJavaDeclaration)))
        bindValue("TParamsOut" ~> (getGenericParams(_, _.getName)))
    }

    private def constructorOut(clazz: ExtendedReferenceCompilationContext): String = {
        //TODO be able to set the constructor with an annotation
        val constructor = clazz.getConstructors()(0)
        constructor.getParameters
                .map(param => s"origin.${param.getName}()")
                .mkString(", ")
    }

    private def typeToJavaDeclaration(tpe: TypeVariable[_]): String = {
        tpe.getName + " extends " + tpe.getBounds.map(_.getTypeName).mkString(" & ")
    }

    implicit private def extractClass(context: ExtendedReferenceCompilationContext): Class[_] = context.clazz

}
