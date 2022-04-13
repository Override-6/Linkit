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

package fr.linkit.engine.internal.language.bhv

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller
import fr.linkit.engine.internal.generation.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.internal.language.bhv.interpreter.{BehaviorFile, BehaviorFileDescriptor, BehaviorFileLambdaExtractor, LangContractDescriptorData}
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageLexer
import fr.linkit.engine.internal.language.bhv.parser.BehaviorFileParser

import scala.collection.mutable
import scala.util.parsing.input.CharSequenceReader

object Contract {

    private final val contracts    = mutable.HashMap.empty[String, PartialContractDescriptorData]
    private final val toPrecompute = mutable.ListBuffer.empty[(String, String)]
    private final val center       = new DefaultCompilerCenter

    def precompute(application: ApplicationContext): Unit = {
        toPrecompute.foreach { case (text, filePath) => partialize(text, filePath, application) }
        toPrecompute.clear()
    }

    private[linkit] def addToPrecompute(text: String, filePath: String): Unit = toPrecompute += ((text, filePath))

    def apply(name: String)(implicit app: ApplicationContext, propertyClass: PropertyClass): LangContractDescriptorData = contracts.get(name) match {
        case Some(partial) =>
            partial(propertyClass)
        case None          =>
            throw new NoSuchElementException(s"Could not find any behavior contract bound with the name '$name'")
    }


    private def partialize(text: String, filePath: String, app: ApplicationContext): PartialContractDescriptorData = {
        val tokens        = BehaviorLanguageLexer.tokenize(new CharSequenceReader(text), filePath)
        val ast           = BehaviorFileParser.parse(tokens)
        val file          = new BehaviorFile(ast, filePath, center)
        val extractor     = new BehaviorFileLambdaExtractor(file)
        val callerFactory = extractor.compileLambdas(app)
        val fileName      = ast.fileName
        val partial       = new PartialContractDescriptorData(file, app, callerFactory)
        contracts.put(fileName, partial)
        partial
    }

    private class PartialContractDescriptorData(file: BehaviorFile, app: ApplicationContext, callerFactory: PropertyClass => MethodCaller) {

        def apply(propertyClass: PropertyClass): LangContractDescriptorData = {
            val caller = callerFactory(propertyClass)
            try {
                val interpreter = new BehaviorFileDescriptor(file, app, propertyClass, caller)
                interpreter.data
            } catch {
                case e: BHVLanguageException => throw new BHVLanguageException(s"in: ${file.filePath}: ${e.getMessage}", e)
            }
        }

    }

}
