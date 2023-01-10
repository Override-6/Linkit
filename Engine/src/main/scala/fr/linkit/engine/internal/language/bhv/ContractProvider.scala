/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.language.bhv

import fr.linkit.api.gnom.cache.sync.contract.Contract
import fr.linkit.api.gnom.cache.sync.contract.behavior.{BHVProperties, ObjectsProperty}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.internal.compilation.access.DefaultCompilerCenter
import fr.linkit.engine.internal.language.bhv.interpreter.{BehaviorFile, BehaviorFileInterpreter, LangContractDescriptorData}
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageLexer
import fr.linkit.engine.internal.language.bhv.parser.BehaviorFileParser

import scala.collection.mutable
import scala.util.parsing.input.CharSequenceReader

object ContractProvider extends Contract.Provider {

    private val properties   = mutable.HashMap.empty[String, BHVProperties]
    private val contracts    = mutable.HashMap.empty[String, ContractHandler]
    private val toPrecompute = mutable.HashSet.empty[(String, String)]
    private val center       = DefaultCompilerCenter

    private[engine] def precompute(): Unit = this.synchronized {
        toPrecompute.foreach { case (text, filePath) => precompute(text, filePath) }
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

    private def precompute(text: String, filePath: String): ContractHandler = {
        val tokens        = BehaviorLanguageLexer.tokenize(new CharSequenceReader(text), filePath)
        val ast           = BehaviorFileParser.parse(tokens)
        val file          = new BehaviorFile(ast, filePath, center)
        val fileName      = ast.fileName
        val partial       = new ContractHandler(file)
        contracts.put(fileName, partial)
        partial
    }

    private class ContractHandler(file: BehaviorFile) {

        private val contracts = mutable.HashMap.empty[String, LangContractDescriptorData]

        def get(propertyName: String): LangContractDescriptorData = {
            contracts.getOrElseUpdate(propertyName, {
                val bhvProperties = properties.getOrElse(propertyName, throw new NoSuchElementException(s"unknown properties '$propertyName'."))
                try {
                    val interpreter = new BehaviorFileInterpreter(file, bhvProperties)
                    interpreter.data
                } catch {
                    case e: BHVLanguageException =>
                        throw new BHVLanguageException(s"in: ${file.filePath}", e)
                }
            })
        }
    }

}
