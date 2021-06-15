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

package fr.linkit.engine.connection.cache.repo.generation

import fr.linkit.api.connection.cache.repo.description.PuppetDescription
import fr.linkit.api.connection.cache.repo.description.PuppetDescription.MethodDescription
import fr.linkit.api.local.generation.TypeVariableTranslator
import fr.linkit.engine.connection.cache.repo.generation.ScalaWrapperClassBlueprint.MethodValueScope
import fr.linkit.engine.local.generation.cbp.{AbstractClassBlueprint, AbstractValueScope, RootValueScope}

import java.util.regex.{MatchResult, Pattern}
import scala.reflect.runtime.universe._

class ScalaWrapperClassBlueprint extends AbstractClassBlueprint[PuppetDescription[_]](classOf[PuppetWrapperClassGenerator].getResourceAsStream("/generation/puppet_wrapper_blueprint.scbp")) {

    override val rootScope: RootValueScope[PuppetDescription[_]] = new RootValueScope[PuppetDescription[_]](blueprint) {
        registerValue("WrappedClassPackage" ~> (_.clazz.getPackageName))
        registerValue("CompileTime" ~~> System.currentTimeMillis())
        registerValue("WrappedClassSimpleName" ~> (_.clazz.getSimpleName))
        registerValue("WrappedClassName" ~> (_.clazz.getTypeName.replaceAll("\\$", ".")))
        registerValue("TParamsIn" ~> getGenericParamsIn)
        registerValue("TParamsOut" ~> getGenericParamsOut)

        bindSubScope(MethodValueScope, (desc, action: MethodDescription => Unit) => {
            desc.listMethods()
                    .distinctBy(_.methodId)
                    .filterNot(desc => {
                        val m = desc.symbol
                        m.isPrivate || m.isStatic || m.isFinal
                    })
                    .foreach(action)
        })

    }

    private def getGenericParamsIn(desc: PuppetDescription[_]): String = {
        val result = TypeVariableTranslator.toJavaDeclaration(desc.clazz.getTypeParameters)
        if (result.isEmpty)
            ""
        else s"[$result]"
    }

    private def getGenericParamsOut(desc: PuppetDescription[_]): String = {
        val result = desc
                .clazz
                .getTypeParameters
                .map(_.getName)
                .mkString(", ")
        if (result.isEmpty)
            ""
        else s"[$result]"
    }
}

object ScalaWrapperClassBlueprint {

    case class MethodValueScope(blueprint: String, pos: Int)
            extends AbstractValueScope[MethodDescription]("INHERITED_METHODS", pos, blueprint) {

        registerValue("ReturnType" ~> getReturnType)
        registerValue("DefaultReturnType" ~> (_.getDefaultTypeReturnValue))
        registerValue("GenericTypesIn" ~> getGenericParamsIn)
        registerValue("GenericTypesOut" ~> getGenericParamsOut)
        registerValue("MethodName" ~> (_.symbol.name.toString))
        registerValue("MethodID" ~> (_.methodId.toString))
        registerValue("InvokeOnlyResult" ~> (_.getDefaultReturnValue))
        registerValue("ParamsIn" ~> (getParameters(_)(_.mkString("(", ", ", ")"), _.mkString(""), true)))
        registerValue("ParamsOut" ~> (getParameters(_)(_.mkString("(", ", ", ")"), _.mkString(""), false)))
        registerValue("ParamsOutArray" ~> (getParameters(_)(_.mkString(", "), _.mkString("Array[Any](", ", ", ")"), false)))

        private def getReturnType(method: MethodDescription): String = {
            val symbol  = method.symbol
            val result  = renderType {
                val base        = method.desc.classType
                val methodOwner = symbol.owner
                val tpe         = symbol.returnType
                tpe.asSeenFrom(base, methodOwner).finalResultType
            }
            val tParams = symbol.typeParams
            val names = getNonAmbiguousTypeParamName(tParams, tParams.length)
            fixAmbiguousNames(result, tParams, names)
        }

        private def getGenericParamsIn(method: MethodDescription): String = {
            val symbol  = method.symbol
            val tParams = symbol.typeParams
            if (tParams.isEmpty)
                return ""
            val names  = getNonAmbiguousTypeParamName(method.desc.classType.typeParams, tParams.size)
            val result = tParams
                    .zip(names)
                    .map(pair => pair._2 + pair._1.typeSignature.asSeenFrom(method.desc.classType, symbol.owner).toString)
                    .mkString("[", ", ", "]")
            fixAmbiguousNames(result, tParams, names)
        }

        def getGenericParamsOut(method: MethodDescription): String = {
            val tParams = method.symbol.typeParams.map(_.name)
            if (tParams.isEmpty)
                return ""
            getNonAmbiguousTypeParamName(method.desc.classType.typeParams, tParams.size)
                    .mkString("[", ", ", "]")
        }

        private def getParameters(method: MethodDescription)(firstMkString: Seq[String] => String,
                                                             secondMkString: Seq[String] => String,
                                                             povIn: Boolean): String = {
            var i = 0

            def n = {
                i += 1
                i
            }

            val symbol  = method.symbol
            val tParams = symbol.typeParams

            def argType(s: Symbol): String = {
                val str = s.typeSignature.asSeenFrom(method.desc.classType, symbol.owner).toString
                if (str.endsWith("*"))
                    if (povIn) {
                        s": Seq[${str.dropRight(1)}]"
                    }
                    else s": _*"
                else if (povIn) ": " + str else ""
            }

            val result = secondMkString {
                symbol
                        .paramLists
                        .map(l => firstMkString(l.map(s => s"arg${n}${argType(s)}")))
            }

            val names = getNonAmbiguousTypeParamName(symbol.typeParams, symbol.typeParams.length)
            fixAmbiguousNames(result, tParams, names)
        }

        private def renderType(tpe: Type): String = {
            val args = tpe.typeArgs
            if (args.isEmpty)
                return tpe.finalResultType.toString
            val v = args
                    .map(renderType)
                    .mkString("[", ", ", "]")
            tpe.typeSymbol.fullName + v
        }

        private def fixAmbiguousNames(str: String, tParams: Seq[Symbol], names: Array[String]): String = {
            var result = str
            for (i <- tParams.indices) {
                val param   = tParams(i)
                val name    = param.name.toString
                val matcher = Pattern.compile(s"(^[\\[,]|[\\[,])$name").matcher(result)
                result = {
                    matcher.replaceAll((result: MatchResult) => {
                        val g = result.group(0)
                        if (g.isEmpty)
                            names(i)
                        else g(0) + names(i)
                    })
                }
            }
            result
        }

        private val pattern = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        private def getNonAmbiguousTypeParamName(forbiddenTypesNames: Seq[Symbol], length: Int): Array[String] = {
            val forbiddenNames = forbiddenTypesNames.map(_.name)
            val params         = new Array[String](length)
            var patternIndex   = 0
            var prefix         = ""
            for (i <- params.indices) {
                var n = patternIndex
                do {
                    n += 1
                    if (n >= pattern.length) {
                        prefix += "A"
                        n = 0
                        patternIndex = 0
                    }
                } while (forbiddenNames.contains(prefix + pattern(n).toString))
                val name = prefix + pattern(n)
                params(i) = name
                patternIndex = n
            }
            params
        }

    }

}
