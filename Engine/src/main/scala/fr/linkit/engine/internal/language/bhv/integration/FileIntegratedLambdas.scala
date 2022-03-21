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

package fr.linkit.engine.internal.language.bhv.integration

import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.engine.internal.generation.compilation.factories.ClassCompilationRequestFactory
import fr.linkit.engine.internal.language.bhv.PropertyClass
import fr.linkit.engine.internal.language.bhv.integration.FileIntegratedLambdas.factory

import scala.collection.mutable

class FileIntegratedLambdas(fileName: String,
                            center: CompilerCenter,
                            imports: Seq[Class[_]],
                            classBlocks: Seq[String]) {

    private val expressions = mutable.HashMap.empty[String, LambdaExpressionInfo]

    /**
     * convert a lambda expression to an actual lambda object.
     * @return the lambda identifier
     * */
    def submitLambda[X](expression: String, name: String, lambdaParams: Class[_]*): Unit = {
        expressions.put(name, LambdaExpressionInfo(name, expression, lambdaParams.toArray))
    }

    def compileLambdas(): PropertyClass => LambdaCaller = {
        val name    = fileName.reverse.takeWhile(_ != '/').reverse.takeWhile(_ != '.')
        val context = LambdaRepositoryContext(name,
            expressions.values.toArray,
            classBlocks.toArray,
            Thread.currentThread().getContextClassLoader,
            imports)
        val clazz   = center
                .processRequest(factory.makeRequest(context))
                .getResult.get
        clazz.getConstructor(classOf[PropertyClass]).newInstance(_)
    }

}

object FileIntegratedLambdas {

    private final val Blueprint = new LambdaRepositoryClassBlueprint(getClass.getResourceAsStream("/generation/scala_lambda_repository.scbp"))
    private final val factory   = new ClassCompilationRequestFactory[LambdaRepositoryContext, LambdaCaller](Blueprint)
}
