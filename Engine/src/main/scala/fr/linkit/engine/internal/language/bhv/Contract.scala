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
import fr.linkit.engine.internal.language.bhv.ast.BehaviorFile
import fr.linkit.engine.internal.language.bhv.interpreter.BehaviorFileInterpreter
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageLexer
import fr.linkit.engine.internal.language.bhv.parser.BehaviorFileParser

import scala.collection.mutable
import scala.util.parsing.input.CharSequenceReader

object Contract {

    private final val contracts = mutable.HashMap.empty[String, ContractDescriptorData]
    private final val center    = new DefaultCompilerCenter

    def apply(file: String)(implicit propertyClass: PropertyClass): ContractDescriptorData = contracts.getOrElse(file, {
        val loader = Thread.currentThread().getContextClassLoader
        val source = new String(loader.getResourceAsStream(file).readAllBytes())
        val tokens = BehaviorLanguageLexer.tokenize(new CharSequenceReader(source), file)
        val ast    = BehaviorFileParser.parse(tokens)
        val cdd    = completeAST(ast, file, propertyClass)
        contracts.put(file, cdd)
        cdd
    })

    private def completeAST(ast: BehaviorFile, fileName: String, propertyClass: PropertyClass): ContractDescriptorData = {
        val interpreter = new BehaviorFileInterpreter(ast, fileName, center, propertyClass)
        interpreter.getData
    }

}
