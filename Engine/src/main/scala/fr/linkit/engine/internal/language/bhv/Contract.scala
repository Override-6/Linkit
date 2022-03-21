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

import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.engine.internal.generation.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.internal.language.bhv.ast.BehaviorFileAST
import fr.linkit.engine.internal.language.bhv.integration.LambdaCaller
import fr.linkit.engine.internal.language.bhv.interpreter.{BehaviorFile, BehaviorFileDescriptor, BehaviorFileLambdaExtractor}
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageLexer
import fr.linkit.engine.internal.language.bhv.parser.BehaviorFileParser

import scala.collection.mutable
import scala.util.parsing.input.CharSequenceReader

object Contract {

    private final val contracts = mutable.HashMap.empty[String, PartialContractDescriptorData]
    private final val center    = new DefaultCompilerCenter

    def apply(file: String)(implicit propertyClass: PropertyClass): ContractDescriptorData = contracts.get(file) match {
        case Some(partial) =>
            partial(propertyClass)
        case None          =>
            val loader = Thread.currentThread().getContextClassLoader
            val source = new String(loader.getResourceAsStream(file).readAllBytes())
            val tokens = BehaviorLanguageLexer.tokenize(new CharSequenceReader(source), file)
            val ast    = BehaviorFileParser.parse(tokens)
            completeAST(ast, file, propertyClass)
    }

    private def completeAST(ast: BehaviorFileAST, fileName: String, propertyClass: PropertyClass): ContractDescriptorData = {
        if (propertyClass == null)
            throw new NullPointerException("property class cannot be null. ")
        val file          = new BehaviorFile(ast)
        val extractor     = new BehaviorFileLambdaExtractor(file, fileName, center)
        val callerFactory = extractor.compileLambdas()
        val partial       = new PartialContractDescriptorData(file, callerFactory)
        contracts.put(fileName, partial)
        partial(propertyClass)
    }

    private class PartialContractDescriptorData(file: BehaviorFile, callerFactory: PropertyClass => LambdaCaller) {

        def apply(propertyClass: PropertyClass): ContractDescriptorData = {
            val caller      = callerFactory(propertyClass)
            val interpreter = new BehaviorFileDescriptor(file, propertyClass, caller)
            interpreter.data
        }

    }

}
