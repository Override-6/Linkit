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
import fr.linkit.api.local.generation.compilation.access.CompilerType
import fr.linkit.engine.connection.cache.repo.generation.ScalaWrapperClassBlueprint.{MethodValueScope, getClassGenericParamsOut}
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.generation.cbp.{AbstractClassBlueprint, AbstractValueScope}
import fr.linkit.engine.local.generation.compilation.access.CommonCompilerTypes

import scala.reflect.runtime.universe._

class ScalaWrapperClassBlueprint extends AbstractClassBlueprint[PuppetDescription[_]](classOf[LinkitApplication].getResourceAsStream("/generation/puppet_wrapper_blueprint.scbp")) {

    override val compilerType: CompilerType = CommonCompilerTypes.Scalac

    override val rootScope: RootValueScope = new RootValueScope {
        bindValue("WrappedClassPackage" ~> (_.clazz.getPackageName))
        bindValue("CompileTime" ~~> System.currentTimeMillis())
        bindValue("WrappedClassSimpleName" ~> (_.clazz.getSimpleName))
        bindValue("WrappedClassName" ~> (_.clazz.getTypeName.replaceAll("\\$", ".")))
        bindValue("TParamsIn" ~> getGenericParamsIn)
        bindValue("TParamsOut" ~> getClassGenericParamsOut)
        bindValue("BustedConstructor" ~> getBustedConstructor)

        bindSubScope(MethodValueScope, (desc, action: MethodDescription => Unit) => {
            desc.listMethods()
                    .distinctBy(_.methodId)
                    .filterNot(m => m.symbol.isSetter || m.symbol.isGetter)
                    .foreach(action)
        })

    }
    private def getBustedConstructor(desc: PuppetDescription[_]): String = {
        val v = desc.tpe
                .decls
                .find(dec => dec.isConstructor && (dec.isPublic || dec.isProtected))
                .fold("") {
                    _.asMethod
                            .paramLists
                            .map(_.map(_ => "nl").mkString("(", ",", ")"))
                            .mkString(",")
                }
        v
    }

    private def getGenericParamsIn(desc: PuppetDescription[_]): String = {
        val result = desc
                .tpe
                .typeParams
                .map(_.asType.toType.finalResultType)
                .mkString(",")
        if (result.isEmpty) ""
        else s"[$result]"
    }

}

object ScalaWrapperClassBlueprint {

    private def getClassGenericParamsOut(desc: PuppetDescription[_]): String = {
        val result = desc
                .clazz
                .getTypeParameters
                .map(_.getName)
                .mkString(",")
        if (result.isEmpty)
            ""
        else s"[$result]"
    }

    case class MethodValueScope(blueprint: String, pos: Int)
            extends AbstractValueScope[MethodDescription]("INHERITED_METHODS", pos, blueprint) {

        bindValue("ReturnType" ~> getReturnType)
        bindValue("DefaultReturnValue" ~> (_.getDefaultTypeReturnValue))
        bindValue("GenericTypesIn" ~> getGenericParamsIn)
        bindValue("GenericTypesOut" ~> getGenericParamsOut)
        bindValue("MethodName" ~> (_.symbol.name.toString))
        bindValue("MethodID" ~> (_.methodId.toString))
        bindValue("InvokeOnlyResult" ~> (_.getDefaultReturnValue))
        bindValue("ParamsIn" ~> (getParameters(_)(_.mkString("(", ", ", ")"), _.mkString(""), true, false)))
        bindValue("ParamsOut" ~> (getParameters(_)(_.mkString("(", ", ", ")"), _.mkString(""), false, true)))
        bindValue("ParamsOutArray" ~> (getParameters(_)(_.mkString(", "), _.mkString("Array(", ", ", ")"), false, false)))

        private def getReturnType(method: MethodDescription): String = {
            val methodSymbol = method.symbol
            val classSymbol  = method.classDesc.tpe.typeSymbol
            val tParams      = classSymbol.typeSignature.typeParams
            val tpe          = methodSymbol.returnType
            val v            = renderTypes(Seq({
                val base        = method.classDesc.tpe
                val methodOwner = methodSymbol.owner
                tpe.asSeenFrom(base, methodOwner).finalResultType
            }), tParams)
                    .mkString("")
            v.replace(classSymbol.fullName + getClassGenericParamsOut(method.classDesc), "this.type")
        }

        private def getGenericParamsIn(method: MethodDescription): String = {
            val symbol      = method.symbol
            val cTypeParams = method
                    .classDesc
                    .tpe
                    .typeParams
            val mTypeParams = symbol.typeParams
            if (mTypeParams.isEmpty)
                return ""
            val v = mTypeParams
                    .map(param => {
                        val v = param
                                .typeSignature
                                .asSeenFrom(method.classDesc.tpe, symbol.owner).finalResultType
                        v
                    })
                    .zipWithIndex
                    .map(pair => {
                        val i   = pair._2
                        val tpe = mTypeParams(i)
                        if (cTypeParams.exists(_.name == tpe.name)) s"$$_$i"
                        else tpe.name.toString + pair._1
                    })
                    .mkString("[", ",", "]")
            v
        }

        def getGenericParamsOut(method: MethodDescription): String = {
            val cTypeParams = method
                    .classDesc
                    .tpe
                    .typeParams
            val mTypeParams = method
                    .symbol
                    .typeSignature
                    .typeParams
                    .zipWithIndex
            if (mTypeParams.isEmpty)
                return ""
            val v = mTypeParams
                    .zipWithIndex
                    .map(pair => {
                        val i    = pair._2
                        val name = mTypeParams(i)._1.name.toString
                        if (cTypeParams.exists(_.name.toString == name)) s"$$_$i"
                        else name
                    })
                    .mkString("[", ",", "]")
            v
        }

        private def getParameters(method: MethodDescription)(firstMkString: Seq[String] => String,
                                                             secondMkString: Seq[String] => String,
                                                             allowTypes: Boolean, allowVarargUpcast: Boolean): String = {
            var i = 0

            def n = {
                i += 1
                i
            }

            val symbol = method.symbol

            def argType(s: Symbol): String = {
                val classType = method.classDesc.tpe
                val str       = renderTypes({
                    Seq(s
                            .typeSignature
                            .asSeenFrom(classType, symbol.owner).finalResultType)
                }, classType.typeParams).head
                if (str.endsWith("*") && allowVarargUpcast) s": _*"
                else if (allowTypes) ": " + str else ""
            }

            val v = secondMkString {
                symbol
                        .paramLists
                        .map(l => {
                            var markedAsImplicit = false
                            firstMkString(l.map(s => {
                                val result = s"arg${n}${argType(s)}"
                                if (allowTypes && !markedAsImplicit && s.isImplicit) {
                                    markedAsImplicit = true
                                    "implicit " + result
                                } else result
                            }))
                        })
            }
            v
        }

        private def renderTypes(types: Seq[Type], classLevelTypes: Seq[Symbol]): Seq[String] = {
            if (types.isEmpty)
                return Seq.empty

            def renderType(t: Type, slot: Symbol, typePos: Int): String = {
                val tpe      = t.finalResultType
                val args     = tpe.typeArgs
                val symbol   = tpe.typeSymbol
                val fullName = symbol.fullName
                if (slot != null && slot.typeSignature.takesTypeArgs) {
                    val ownerType       = slot.typeSignature
                    val typeDeclaration = ownerType.typeParams(typePos)
                    if (typeDeclaration.typeSignature.takesTypeArgs)
                        return if (symbol.isParameter) symbol.name.toString else fullName
                }
                lazy val value = {
                    if (args.isEmpty) ""
                    else args
                            .zipWithIndex
                            .map(pair => renderType(pair._1, symbol, pair._2))
                            .mkString("[", ",", "]")
                }
                if (fullName.startsWith("scala.<"))
                    return tpe.toString
                val idx = classLevelTypes.indexWhere(_.name.toString == symbol.name.toString)
                if (idx != -1 && classLevelTypes.forall(_.fullName != fullName))
                    return s"$$_$idx" + (if (args.nonEmpty) ' ' + value else "")
                if (tpe.toString.endsWith("[_]") || fullName.endsWith("<refinement>"))
                    return tpe.toString
                val result = (if (symbol.isParameter) symbol.name.toString else fullName) + value
                result
            }

            for (tpe <- types) yield renderType(tpe, null, 0)
        }

    }

}
