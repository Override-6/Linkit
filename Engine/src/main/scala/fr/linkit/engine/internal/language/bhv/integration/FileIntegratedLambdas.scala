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

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.api.internal.system.AppLoggers
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.application.resource.external.LocalResourceFolder
import fr.linkit.engine.internal.generation.compilation.factories.ClassCompilationRequestFactory
import fr.linkit.engine.internal.generation.compilation.resource.CachedClassFolderResource
import fr.linkit.engine.internal.language.bhv.PropertyClass
import fr.linkit.engine.internal.language.bhv.ast.BehaviorFileAST

import scala.collection.mutable

class FileIntegratedLambdas(center: CompilerCenter,
                            ast: BehaviorFileAST) {

    private val imports     = mutable.HashSet.empty[Class[_]]
    private val classBlocks = ast.codeBlocks.map(_.sourceCode)
    private val expressions = mutable.HashMap.empty[String, LambdaExpressionInfo]
    private val fileName = ast.fileName

    def addImportedClass(clazz: Class[_]): Unit = imports += clazz

    /**
     * convert a lambda expression to an actual lambda object.
     *
     * @return the lambda identifier
     * */
    def submitLambda[X](expression: String, name: String, lambdaParams: Class[_]*): Unit = {
        expressions.put(name, LambdaExpressionInfo(name, expression, lambdaParams.toArray))
    }

    def compileLambdas(app: ApplicationContext): PropertyClass => MethodCaller = {

        val name    = fileName.reverse.takeWhile(_ != '/').reverse.takeWhile(_ != '.')
        val context = LambdaRepositoryContext(name,
            expressions.values.toArray,
            classBlocks.toArray,
            Thread.currentThread().getContextClassLoader,
            imports.toSeq)

        val resource = app.getAppResources
            .getOrOpen[LocalResourceFolder](LinkitApplication.getProperty("compilation.working_dir.classes"))
            .getEntry
            .getOrAttachRepresentation[CachedClassFolderResource[MethodCaller]]("lambdas")

        val clazz       = resource
            .findClass(context.classPackage + "." + context.className, context.parentLoader)
            .getOrElse(genClass(context))
        val constructor = clazz.getConstructor(classOf[PropertyClass])
        constructor.newInstance(_)
    }

    private def genClass(context: LambdaRepositoryContext): Class[_ <: MethodCaller] = {
        val result = center
            .processRequest {
                AppLoggers.Compilation.info(s"Compiling Lambdas Class for behavior file '$fileName'...")
                FileIntegratedLambdas.CompilationRequestFactory.makeRequest(context)
            }
        AppLoggers.Compilation.info(s"Compilation done in ${result.getCompileTime} ms.")
        result.getValue.get
    }

}

object FileIntegratedLambdas {

    private final val Blueprint                 = new LambdaCallerClassBlueprint(getClass.getResourceAsStream("/generation/scala_lambda_repository.scbp"))
    private final val CompilationRequestFactory = new ClassCompilationRequestFactory[LambdaRepositoryContext, MethodCaller](Blueprint)
}
