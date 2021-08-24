/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.local.language.bhv.descriptor.clazz

import fr.linkit.engine.connection.cache.obj.description.SimpleSyncObjectSuperClassDescription
import fr.linkit.engine.local.language.bhv.{BehaviorFileException, BehaviorFileSyntaxException}
import fr.linkit.engine.local.language.bhv.descriptor.FieldBehaviorDescriptionResult
import fr.linkit.engine.local.language.bhv.descriptor.method.{MethodBehaviorDescriptionException, MethodBehaviorDescriptionResult, MethodDescriptor}
import fr.linkit.engine.local.language.bhv.descriptor.method.MethodBehaviorDescriptionResult
import fr.linkit.engine.local.language.bhv.BehaviorFileException

import java.util.Scanner
import scala.collection.mutable

class ClassDescriptionResultBuilder(scanner: Scanner, classDesc: SimpleSyncObjectSuperClassDescription[_]) {

    private val clazz                                                  = classDesc.clazz
    private val className                                              = clazz.getName
    private var defaultFieldBehavior : FieldBehaviorDescriptionResult  = _
    private var defaultMethodBehavior: MethodBehaviorDescriptionResult = _

    private val fieldMap  = mutable.HashMap.empty[String, FieldBehaviorDescriptionResult]
    private val methodMap = mutable.HashMap.empty[String, MethodBehaviorDescriptionResult]

    private val methodDescs = classDesc.listMethods()
    private val fieldDescs  = classDesc.listFields()

    launchParsing()

    def getMethodResult(name: String): Option[MethodBehaviorDescriptionResult] = methodMap.get(name)

    def getFieldResult(name: String): Option[FieldBehaviorDescriptionResult] = fieldMap.get(name)

    private def launchParsing(): Unit = {
        if (scanner.next() != "{")
            throw new BehaviorFileSyntaxException("Required '{' character after class descriptor header.")
        var word = scanner.next()
        while (word != "}") {
            word match {
                case "forall"  => parseForall()
                case "enable"  => parseEnabledMember()
                case "disable" => parseDisabledMember()
            }
            word = scanner.next()
        }
    }

    private def parseForall(): Unit = {
        @inline
        def fail(memberName: String): Nothing = {
            throw new BehaviorFileException(s"'forall $memberName' already defined for class $className description.")
        }

        val memberType = scanner.next()
        if (scanner.next() != "->")
            throw new BehaviorFileSyntaxException(s"Expected '->' after 'forall $memberType'.")
        memberType match {
            case "field"  =>
                if (defaultFieldBehavior != null)
                    fail("field")
                defaultFieldBehavior = parseField()
            case "method" =>
                if (defaultMethodBehavior != null)
                    fail("method")
                defaultMethodBehavior = parseMethodGeneric()
        }
    }

    private def parseField(): FieldBehaviorDescriptionResult = {
        val word  = scanner.next()
        val isEOL = scanner.nextLine().isEmpty
        word match {
            case "disable" =>
                if (!isEOL)
                    throw new BehaviorFileSyntaxException("Disabled fields don't needs more instruction (end of line not reached)")
                FieldBehaviorDescriptionResult.Disabled
            case "enabled" =>
                if (!isEOL)
                    throw new UnsupportedOperationException("Fields can either be enabled or disabled, further enabled description is not yet supported.")
                new FieldBehaviorDescriptionResult(isEnabled = true)
        }
    }

    private def parseMethodGeneric(): MethodBehaviorDescriptionResult = {
        scanner.next() match {
            case "disable" => MethodBehaviorDescriptionResult.Disabled
            case "enable"  => parseMethod(null)
            case other     => throw new MethodBehaviorDescriptionException(s"Expected 'disable' or 'enable' for generic method descriptor but found '$other'.")
        }
    }

    private def parseMethod(methodName: String): MethodBehaviorDescriptionResult = {
        val methodDesc = if (methodName == null) null else methodDescs.find(_.method.getName == methodName).getOrElse {
            throw new MethodBehaviorDescriptionException(s"Unknown method $methodName in class $className")
        }
        new MethodDescriptor(methodDesc, this).describe(scanner)
    }

    private def parseEnabledMember(): Unit = {
        def putMember[M](memberType: String, memberName: String, map: mutable.HashMap[String, M], parseAction: => M): Unit = {
            if (map.contains(memberName))
                throw new BehaviorFileException(s"$memberType '$memberName' for $clazz was already described.")
            val fieldDescription = parseAction
            map.put(memberName, fieldDescription)
        }

        scanner.next() match {
            case "field"    =>
                putMember("Field", scanner.next(), fieldMap, parseField())
            case "method"   =>
                val methodName = scanner.next()
                putMember("Method", methodName, methodMap, parseMethod(methodName))
            case methodName =>
                putMember("Method", methodName, methodMap, parseMethod(methodName))
        }
    }

    private def parseDisabledMember(): Unit = {
        val word = scanner.next()
        word match {
            case "field"    =>
                val memberName = scanner.next()
                if (scanner.nextLine().nonEmpty)
                    throw new UnsupportedOperationException("Fields can either be enabled or disabled, further enabled description is not yet supported.")
                fieldMap.put(memberName, FieldBehaviorDescriptionResult.Disabled)
            case "method"   =>
                val memberName = scanner.next()
                methodMap.put(memberName, MethodBehaviorDescriptionResult.Disabled)
            case methodName =>
                methodMap.put(methodName, MethodBehaviorDescriptionResult.Disabled)
        }
    }

    def result(): ClassDescriptionResult = {
        null
    }
}
