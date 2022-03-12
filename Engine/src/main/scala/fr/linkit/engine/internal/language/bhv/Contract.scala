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

import fr.linkit.api.gnom.cache.sync.contract.descriptors.ContractDescriptorData
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.engine.internal.language.bhv.ast.BehaviorFile
import fr.linkit.engine.internal.language.bhv.interpretation.BehaviorFileInterpreter
import fr.linkit.engine.internal.language.bhv.lexer.BehaviorLanguageLexer
import fr.linkit.engine.internal.language.bhv.parser.BehaviorLanguageParser

import scala.collection.mutable
import scala.util.parsing.input.CharSequenceReader

object Contract {

    private final val contracts                      = mutable.HashMap.empty[String, ContractDescriptorData]
    private final var center: Option[CompilerCenter] = None

    private[linkit] def initCenter(center: CompilerCenter): Unit = {
        if (this.center.isDefined)
            throw new IllegalAccessException("center already initialized")
        if (center == null)
            throw new NullPointerException
        this.center = Some(center)
    }


    def apply(file: String)(implicit propertyClass: PropertyClass): ContractDescriptorData = contracts.getOrElse(file, {
        val source = new String(getClass.getResourceAsStream(file).readAllBytes())
        val tokens = BehaviorLanguageLexer.tokenize(new CharSequenceReader(source))
        val ast    = BehaviorLanguageParser.parse(tokens)
        val cdd    = completeAST(ast, file, propertyClass)
        contracts.put(file, cdd)
        cdd
    })

    private def completeAST(ast: BehaviorFile, fileName: String, propertyClass: PropertyClass): ContractDescriptorData = {
        val center      = this.center.getOrElse(throw new UnsupportedOperationException("Compiler center not initialised."))
        val interpreter = new BehaviorFileInterpreter(ast, fileName, center, propertyClass)
        interpreter.getData
    }

}
