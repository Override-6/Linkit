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
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.internal.generation.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.internal.language.bhv.interpreter.{BehaviorFile, BehaviorFileDescriptor, BehaviorFileLambdaExtractor, LangContractDescriptorData}
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageLexer
import fr.linkit.engine.internal.language.bhv.parser.BehaviorFileParser

import scala.collection.mutable
import scala.util.parsing.input.CharSequenceReader

object Contract {
    
    private val properties   = mutable.HashMap.empty[String, BHVProperties]
    private val contracts    = mutable.HashMap.empty[String, ContractHandler]
    private val toPrecompute = mutable.HashSet.empty[(String, String)]
    private val center       = new DefaultCompilerCenter
    
    private lazy val app = LinkitApplication.getApplication
    
    def precompute(application: ApplicationContext): Unit = {
        toPrecompute.foreach { case (text, filePath) => precompute(text, filePath, application) }
        toPrecompute.clear()
    }
    
    def registerProperties(properties: BHVProperties): Unit = {
        this.properties.get(properties.name) match {
            case Some(value) =>
                if (value == properties) return
                throw new IllegalArgumentException(s"properties' name is already registered ($properties)")
            case None        =>
                this.properties.put(properties.name, properties)
        }
    }
    
    private[linkit] def addToPrecompute(text: String, filePath: String): Unit = toPrecompute += ((text, filePath))
    
    def apply(name: String, properties: BHVProperties): LangContractDescriptorData = {
        this.properties.get(properties.name) match {
            case Some(value) =>
                if (value != properties) throw new IllegalArgumentException("properties' name is already registered")
            case None        =>
                registerProperties(properties)
        }
        apply(name, properties.name)
    }
    
    def apply(name: String, propertiesName: String): LangContractDescriptorData = {
        contracts.get(name) match {
            case Some(handler) =>
                handler.get(propertiesName)
            case None          =>
                throw new NoSuchElementException(s"Could not find any behavior contract bound with the name '$name'")
        }
    }
    
    def apply(name: String): LangContractDescriptorData = {
        apply(name, ObjectsProperty.empty)
    }
    
    private def precompute(text: String, filePath: String, app: ApplicationContext): ContractHandler = {
        val tokens        = BehaviorLanguageLexer.tokenize(new CharSequenceReader(text), filePath)
        val ast           = BehaviorFileParser.parse(tokens)
        val file          = new BehaviorFile(ast, filePath, center)
        val extractor     = new BehaviorFileLambdaExtractor(file)
        val callerFactory = extractor.compileLambdas(app)
        val fileName      = ast.fileName
        val partial       = new ContractHandler(file, callerFactory)
        contracts.put(fileName, partial)
        partial
    }
    
    private class ContractHandler(file: BehaviorFile, callerFactory: BHVProperties => MethodCaller) {
        
        private val callers   = mutable.HashMap.empty[String, MethodCaller]
        private val contracts = mutable.HashMap.empty[String, LangContractDescriptorData]
        
        def get(propertyName: String): LangContractDescriptorData = {
            contracts.getOrElseUpdate(propertyName, {
                val bhvProperties = properties.getOrElse(propertyName, throw new NoSuchElementException(s"unknown properties '$propertyName'."))
                val caller        = callers.getOrElseUpdate(propertyName, callerFactory(bhvProperties))
                try {
                    val interpreter = new BehaviorFileDescriptor(file, app, bhvProperties, caller)
                    interpreter.data
                } catch {
                    case e: BHVLanguageException =>
                        throw new BHVLanguageException(s"in: ${file.filePath}: ${e.getMessage}", e)
                }
            })
        }
    }
    
}
