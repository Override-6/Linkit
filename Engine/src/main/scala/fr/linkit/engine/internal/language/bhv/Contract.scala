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
import fr.linkit.engine.internal.generation.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.internal.language.bhv.ast.BehaviorFileAST
import fr.linkit.engine.internal.language.bhv.integration.LambdaCaller
import fr.linkit.engine.internal.language.bhv.interpreter.{BehaviorFile, BehaviorFileDescriptor, BehaviorFileLambdaExtractor, LangContractDescriptorData}
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageLexer
import fr.linkit.engine.internal.language.bhv.parser.BehaviorFileParser

import java.io.File
import java.net.URL
import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable
import scala.util.parsing.input.CharSequenceReader

object Contract {

    private final val contracts = mutable.HashMap.empty[String, PartialContractDescriptorData]
    private final val center    = new DefaultCompilerCenter

    def apply(url: URL)(implicit app: ApplicationContext, propertyClass: PropertyClass): LangContractDescriptorData = contracts.get(url.getFile) match {
        case Some(partial) =>
            partial(propertyClass)
        case None          =>
            val stream = url.openStream()
            val result = fromText(new String(stream.readAllBytes()), url.getFile)
            stream.close()
            result
    }

    //FIXME Using URL is ok but when a contract must be sent to another computer,
    // it becomes complex to handle where to find the computer's equivalent source
    // as the bhv file is certainly not stored at the same path.
    def apply(url: String)(implicit app: ApplicationContext, propertyClass: PropertyClass): LangContractDescriptorData = {
        val loader = Thread.currentThread().getContextClassLoader
        if (url.contains(":")) {//it's an absolute file path
            val pathStr = if (url.head == '/') url.tail else url
            fromText(Files.readString(Path.of(pathStr)), url)
        } else apply(loader.getResource(url))
    }

    private def fromText(text: String, source: String)(implicit app: ApplicationContext, propertyClass: PropertyClass): LangContractDescriptorData = {
        val tokens = BehaviorLanguageLexer.tokenize(new CharSequenceReader(text), source)
        val ast    = BehaviorFileParser.parse(tokens)
        completeAST(ast, source, propertyClass, app)
    }

    private def completeAST(ast: BehaviorFileAST, filePath: String, propertyClass: PropertyClass, app: ApplicationContext): LangContractDescriptorData = {
        if (propertyClass == null)
            throw new NullPointerException("property class cannot be null. ")
        val file          = new BehaviorFile(ast, filePath)
        val extractor     = new BehaviorFileLambdaExtractor(file, filePath, center)
        val callerFactory = extractor.compileLambdas(app)
        val partial       = new PartialContractDescriptorData(file, app, callerFactory)
        contracts.put(filePath, partial)
        partial(propertyClass)
    }

    private class PartialContractDescriptorData(file: BehaviorFile, app: ApplicationContext, callerFactory: PropertyClass => LambdaCaller) {

        def apply(propertyClass: PropertyClass): LangContractDescriptorData = {
            val caller = callerFactory(propertyClass)
            try {
                val interpreter = new BehaviorFileDescriptor(file, app, propertyClass, caller)
                interpreter.data
            } catch {
                case e: BHVLanguageException => throw new BHVLanguageException(s"in: ${file.source}: ${e.getMessage}", e)
            }
        }

    }

}
