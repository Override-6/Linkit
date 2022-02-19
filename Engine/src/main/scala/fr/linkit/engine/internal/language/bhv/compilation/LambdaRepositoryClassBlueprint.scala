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

package fr.linkit.engine.internal.language.bhv.compilation

import fr.linkit.api.internal.generation.compilation.access.CompilerType
import fr.linkit.engine.gnom.cache.sync.generation.sync.ScalaBlueprintUtilities.getParameters
import fr.linkit.engine.internal.generation.compilation.access.CommonCompilerType
import fr.linkit.engine.internal.language.cbp.{AbstractClassBlueprint, AbstractValueScope}

import java.io.InputStream

class LambdaRepositoryClassBlueprint(bp: InputStream) extends AbstractClassBlueprint[LambdaRepositoryContext](bp) {

    override val compilerType: CompilerType   = CommonCompilerType.Scalac
    override val rootScope   : RootValueScope = new RootValueScope {
        bindSubScope(new LambdaMethodScope(_, _), (context, action: LambdaExpressionInfo => Unit) => context.expressions.foreach(action))
    }

    private class LambdaMethodScope(bp: String, pos: Int)
            extends AbstractValueScope[LambdaExpressionInfo]("LAMBDA_METHODS", pos, bp) {

        bindValue("MethodID" ~> (_.id.toString))
        bindValue("ParamsIn" ~> (e => getParameters(e.paramTypes, true)))
        bindValue("LambdaExpression" ~> (_.expression))
        bindValue("ParamsOut" ~> (_.paramTypes.zipWithIndex.map { case (_, i) => s"args($i)" }.mkString(",")))
    }

}
