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
import fr.linkit.engine.connection.cache.repo.generation.ScalaWrapperClassBlueprint.{MethodValueScope, getClassGenericParamsOut}
import fr.linkit.engine.local.generation.cbp.{AbstractClassBlueprint, AbstractValueScope, RootValueScope}

import scala.collection.mutable
import scala.reflect.runtime.universe._

class ScalaWrapperClassBlueprint extends AbstractClassBlueprint[PuppetDescription[_]](classOf[PuppetWrapperClassGenerator].getResourceAsStream("/generation/puppet_wrapper_blueprint.scbp")) {

    override val rootScope: RootValueScope[PuppetDescription[_]] = new RootValueScope[PuppetDescription[_]](blueprint) {
        registerValue("WrappedClassPackage" ~> (_.clazz.getPackageName))
        registerValue("CompileTime" ~~> System.currentTimeMillis())
        registerValue("WrappedClassSimpleName" ~> (_.clazz.getSimpleName))
        registerValue("WrappedClassName" ~> (_.clazz.getTypeName.replaceAll("\\$", ".")))
        registerValue("TParamsIn" ~> getGenericParamsIn)
        registerValue("TParamsOut" ~> getClassGenericParamsOut)

        bindSubScope(MethodValueScope, (desc, action: MethodDescription => Unit) => {
            desc.listMethods()
                    .distinctBy(_.methodId)
                    .foreach(action)
        })

    }

    private def getGenericParamsIn(desc: PuppetDescription[_]): String = {
        val result = TypeVariableTranslator.toJavaDeclaration(desc.clazz.getTypeParameters)
        if (result.isEmpty)
            ""
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

        registerValue("ReturnType" ~> getReturnType)
        registerValue("DefaultReturnValue" ~> (_.getDefaultTypeReturnValue))
        registerValue("GenericTypesIn" ~> getGenericParamsIn)
        registerValue("GenericTypesOut" ~> getGenericParamsOut)
        registerValue("MethodName" ~> (_.symbol.name.toString))
        registerValue("MethodID" ~> (_.methodId.toString))
        registerValue("InvokeOnlyResult" ~> (_.getDefaultReturnValue))
        registerValue("ParamsIn" ~> (getParameters(_)(_.mkString("(", ", ", ")"), _.mkString(""), true, false)))
        registerValue("ParamsOut" ~> (getParameters(_)(_.mkString("(", ", ", ")"), _.mkString(""), false, true)))
        registerValue("ParamsOutArray" ~> (getParameters(_)(_.mkString(", "), _.mkString("Array(", ", ", ")"), false, false)))

        private def getReturnType(method: MethodDescription): String = {
            val methodSymbol = method.symbol
            val classSymbol  = method.classDesc.tpe.typeSymbol
            val tParams      = classSymbol.typeSignature.typeParams
            val tpe          = methodSymbol.returnType
            val v            = renderTypes(Seq(( {
                val base        = method.classDesc.tpe
                val methodOwner = methodSymbol.owner
                tpe.asSeenFrom(base, methodOwner).finalResultType
            }, tParams.indexOf(tpe))), tParams)
                    .mkString("")
            v.replace(classSymbol.fullName + getClassGenericParamsOut(method.classDesc), "this.type")
        }

        private def getGenericParamsIn(method: MethodDescription): String = {
            val symbol      = method.symbol
            val cTypeParams = method
                    .classDesc
                    .tpe
                    .typeParams
            val mTypeParams = symbol.typeParams.zipWithIndex
            if (mTypeParams.isEmpty)
                return ""
            val v = mTypeParams
                    .map(pair =>
                        renderTypes(
                            Seq((pair._1.typeSignature.asSeenFrom(method.classDesc.tpe, symbol.owner).finalResultType, pair._2)),
                            symbol.owner.asClass.typeParams).head
                    )
                    .zipWithIndex
                    .map(pair => {
                        val i   = pair._2
                        val tpe = mTypeParams(i)._1
                        if (cTypeParams.exists(_.name == tpe.name)) s"$$_$i" + pair._2
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
                        val i   = pair._2
                        val tpe = mTypeParams(i)._1
                        if (cTypeParams.contains(tpe)) s"$$_$i"
                        else tpe.name
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

            def argType(s: Symbol, position: Int): String = {
                val str = renderTypes({
                    Seq(s
                            .typeSignature
                            .asSeenFrom(method.classDesc.tpe, symbol.owner).finalResultType -> position)
                }, symbol.owner.asClass.typeParams).head
                if (str.endsWith("*") && allowVarargUpcast) s": _*"
                else if (allowTypes) ": " + str else ""
            }

            val v = secondMkString {
                symbol
                        .paramLists
                        .map(l => {
                            var markedAsImplicit = false
                            firstMkString(l.map(s => {
                                val result = s"arg${n}${argType(s, i)}"
                                if (allowTypes && !markedAsImplicit && s.isImplicit) {
                                    markedAsImplicit = true
                                    "implicit " + result
                                } else result
                            }))
                        })
            }
            v
        }

        private def renderTypes(types: Seq[(Type, Int)], classLevelTypes: Seq[Symbol]): Seq[String] = {
            if (types.isEmpty)
                return Seq.empty

            def renderType(tpe: Type, owner: Symbol, typePos: Int, declarationPos: Int): String = {
                val args = tpe.typeArgs
                val name = tpe.typeSymbol.fullName
                if (owner != null && owner.typeSignature.takesTypeArgs) {
                    val ownerType       = owner.typeSignature
                    val typeDeclaration = ownerType.typeParams(typePos)
                    if (typeDeclaration.typeSignature.takesTypeArgs)
                        return name
                }
                if (name.startsWith("scala.<"))
                    return tpe.toString
                if (args.isEmpty)
                    return tpe.finalResultType.toString
                if (tpe.toString.endsWith("[_]"))
                    return tpe.toString
                val value = {
                    args
                            .zipWithIndex
                            .map(pair => renderType(pair._1, tpe.typeSymbol, pair._2, declarationPos))
                            .mkString("[", ",", "]")
                }
                if (classLevelTypes.exists(_.fullName == tpe.typeSymbol.fullName)) {
                    return s"$$_$declarationPos " + value
                }
                name + value
            }

            for ((tpe, i) <- types) yield renderType(tpe, null, 0, i)
        }

    }

}
