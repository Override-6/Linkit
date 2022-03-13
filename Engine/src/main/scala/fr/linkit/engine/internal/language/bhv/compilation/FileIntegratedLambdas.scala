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

import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.engine.internal.generation.compilation.factories.ClassCompilationRequestFactory
import fr.linkit.engine.internal.language.bhv.compilation.FileIntegratedLambdas.factory

import scala.collection.mutable

class FileIntegratedLambdas(fileName: String, center: CompilerCenter, imports: Seq[Class[_]]) {

    private val expressions                    = mutable.HashMap.empty[Int, LambdaExpressionInfo]
    private var repo: Option[LambdaRepository] = None

    /**
     * convert a lambda expression to an actual lambda object.
     *
     * */
    def submitLambda[X](expression: String, lambdaParams: Class[_]*): Array[Any] => X = {
        val id = expression.hashCode
        expressions.put(id, LambdaExpressionInfo(id, expression, lambdaParams.toArray))
        args => {
            val repo = this.repo.getOrElse(throw new UnsupportedOperationException("Lambda Repository not yet compiled."))
            repo.call(id, args).asInstanceOf[X]
        }
    }

    def compileLambdas(): Unit = {
        val context = LambdaRepositoryContext(fileName, expressions.values.toArray, Thread.currentThread().getContextClassLoader)
        val clazz   = center.processRequest(factory.makeRequest(context))
                .getResult.get
        repo = Some(clazz.getConstructor().newInstance())
    }

}

object FileIntegratedLambdas {

    private final val Blueprint = new LambdaRepositoryClassBlueprint(getClass.getResourceAsStream("generation/scala_lambda_repository.scbp"))
    private final val factory   = new ClassCompilationRequestFactory[LambdaRepositoryContext, LambdaRepository](Blueprint)
}
