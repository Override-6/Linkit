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

package fr.linkit.engine.internal.language.bhv.interpreter

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.engine.gnom.cache.sync.contract.description.{SyncObjectDescription, SyncStaticsDescription}
import fr.linkit.engine.internal.language.bhv.PropertyClass
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.integration.{FileIntegratedLambdas, LambdaCaller}

class BehaviorFileLambdaExtractor(file: BehaviorFile, fileName: String) {

    private val ast     = file.ast
    private val lambdas = file.lambdas

    ast.typesModifiers.foreach(t => submitAll("type", t.typeName, t))
    ast.valueModifiers.foreach(v => submitAll(s"value_${v.name}", v.typeName, v))
    ast.classDescriptions.foreach(classDesc => {
        classDesc.methods.foreach {
            case desc: AttributedEnabledMethodDescription => extractMethodModifiers(desc, classDesc)
            case _                                        =>
        }
    })

    def compileLambdas(app: ApplicationContext): PropertyClass => LambdaCaller = lambdas.compileLambdas(app)

    private def extractMethodModifiers(desc: AttributedEnabledMethodDescription, classDesc: ClassDescription): Unit = {
        val signature        = desc.signature
        desc.modifiers.foreach {
            case exp: ModifierExpression =>
                val className = exp.target match {
                    case "returnvalue" =>
                        val classSystemDesc  = classDesc.head.kind match {
                            case _@(RegularDescription | _: MirroringDescription) => SyncObjectDescription(file.findClass(classDesc.head.className))
                            case StaticsDescription                               => SyncStaticsDescription(file.findClass(classDesc.head.className))
                        }
                        file.getMethodDescFromSignature(classDesc.head.kind, signature, classSystemDesc).javaMethod.getReturnType.getName
                    case paramName =>
                        file.findClass(signature.params.find(_.name.contains(paramName)).get.tpe).getName
                }
                submitAll(s"method_${exp.target}_${signature.methodName}_${signature.hashCode()}", className, exp)
            case _ => //fallback will be for modifier references, they don't hold any lambda expression so let's skip them
        }
    }

    private def submitAll(tpe: String, className: String, holder: LambdaExpressionHolder): Unit = {
        submitLambda(holder.in, tpe, className)
        submitLambda(holder.out, tpe, className)
    }

    private def submitLambda(exp: Option[LambdaExpression], tpe: String, className: String): Unit = exp.foreach { exp =>
        val clazz = file.findClass(className)
        val classNameFormatted = file.formatClassName(clazz)
        val kind  = exp.kind match {
            case In  => "in"
            case Out => "out"
        }
        lambdas.submitLambda(exp.block.sourceCode, s"${tpe}_${kind}_$classNameFormatted", clazz, classOf[Engine])
    }

}
