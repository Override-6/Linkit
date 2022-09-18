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

package fr.linkit.engine.internal.language.bhv.integration

import fr.linkit.api.internal.generation.compilation.access.CompilerType
import fr.linkit.engine.gnom.cache.sync.generation.ScalaBlueprintUtilities.{getParameters, toScalaString}
import fr.linkit.engine.internal.generation.compilation.access.CommonCompilerType
import fr.linkit.engine.internal.language.cbp.{AbstractClassBlueprint, AbstractValueScope}

import java.io.InputStream

class LambdaCallerClassBlueprint(bp: InputStream) extends AbstractClassBlueprint[LambdaRepositoryContext](bp) {

    override val compilerType: CompilerType   = CommonCompilerType.Scalac
    override val rootScope   : RootValueScope = new RootValueScope {
        bindValue("Imports" ~> (_.importedClasses.map(cl => s"import ${cl.getName}\n").mkString("")))
        bindValue("Blocks" ~> (_.blocks.mkString("\n")))
        bindSubScope("LAMBDA_METHODS", new LambdaMethodScope(_, _, _),
            (context, action: LambdaExpressionInfo => Unit) => context.expressions.foreach(action))
    }

    private class LambdaMethodScope(name: String, bp: String, pos: Int)
            extends AbstractValueScope[LambdaExpressionInfo](name, bp, pos) {

        bindValue("MethodName" ~> (_.name))
        bindValue("ParamsIn" ~> (e => getParameters(e.paramTypes, true)))
        bindValue("LambdaExpression" ~> (_.expression))
        bindValue("ParamsOut" ~> (_.paramTypes.zipWithIndex.map { case (c, i) => s"args($i).asInstanceOf[${toScalaString(c)}]" }.mkString(",")))
        bindValue("ParamTypes" ~> (_.paramTypes.map(toScalaString).mkString(",")))
    }

}

object LambdaCallerClassBlueprint {

    def getPropertyAccessCodeString(name: String, tpe: String): String = {
        s"getProperty[$tpe](\"${name.replaceAll("\"", "\\\"")}\")"
    }
}
