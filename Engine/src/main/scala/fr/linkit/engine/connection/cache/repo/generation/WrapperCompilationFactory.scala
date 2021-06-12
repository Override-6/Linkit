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
import fr.linkit.api.connection.cache.repo.generation.GeneratedClassClassLoader
import fr.linkit.api.local.generation.TypeVariableTranslator
import fr.linkit.api.local.generation.compilation.{CompilationRequest, CompilationRequestFactory, CompilationResult}
import fr.linkit.engine.connection.cache.repo.generation.WrapperCompilationFactory.ClassValueScope
import fr.linkit.engine.local.LinkitApplication
import fr.linkit.engine.local.generation.cbp.{AbstractValueScope, SimpleClassBlueprint}
import fr.linkit.engine.local.generation.compilation.CompilationRequestBuilder
import fr.linkit.engine.local.generation.compilation.access.CommonCompilerTypes

import java.lang.reflect.{Modifier, Type}
import java.nio.file.Path

class WrapperCompilationFactory extends CompilationRequestFactory[PuppetDescription[_], Class[_]] {

    override val defaultWorkingDirectory: Path = Path.of(LinkitApplication.getProperty("compilation.working_dir"))

    override def makeRequest(context: PuppetDescription[_], workingDirectory: Path): CompilationRequest[Class[_]] = {
        val req = createMultiRequest(Seq(context), workingDirectory)

        new CompilationRequestBuilder[Class[_]] {
            override var sourceCodes     : Seq[(String, String)] = req.sourceCodes
            override val workingDirectory: Path                  = req.workingDirectory

            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Class[_]] = {
                new CompilationResult[Class[_]] {
                    override def get: Class[_] = req.conclude(outs, compilationTime).get.head

                    override def getCompileTime: Long = compilationTime

                    override def getRequest: CompilationRequest[_] = req
                }
            }
        }
    }

    override def makeMultiRequest(contexts: Seq[PuppetDescription[_]], workingDirectory: Path): CompilationRequest[Seq[Class[_]]] = {
        createMultiRequest(contexts, workingDirectory)
    }

    private def createMultiRequest(contexts: Seq[PuppetDescription[_]], workingDir: Path): CompilationRequestBuilder[Seq[Class[_]]] = {
        val bp = blueprints(CommonCompilerTypes.Scalac)
        new CompilationRequestBuilder[Seq[Class[_]]] { request =>
            override val workingDirectory: Path                  = workingDir
            override var sourceCodes     : Seq[(String, String)] = {
                contexts.map(desc => (toWrapperClassName(desc.clazz.getName), bp.toClassSource(desc)))
            }

            override def conclude(outs: Seq[Path], compilationTime: Long): CompilationResult[Seq[Class[_]]] = {
                new CompilationResult[Seq[Class[_]]] {
                    override def get: Seq[Class[_]] = {
                        contexts
                                .filter(desc => outs.contains(workingDir.resolve(toWrapperClassName(desc.clazz.getName))))
                                .map { desc =>
                                    val clazz            = desc.clazz
                                    val wrapperClassName = toWrapperClassName(clazz.getName)
                                    new GeneratedClassClassLoader(workingDir, clazz.getClassLoader).loadClass(wrapperClassName)
                                }
                    }

                    override def getCompileTime: Long = compilationTime

                    override def getRequest: CompilationRequest[_] = request
                }
            }
        }
    }

    {
        val scbp = new SimpleClassBlueprint(classOf[PuppetWrapperClassGenerator].getResourceAsStream("/generation/puppet_wrapper_blueprint.scbp"), new ClassValueScope(_))
        registerClassBlueprint(CommonCompilerTypes.Scalac, scbp)
    }

}

object WrapperCompilationFactory {

    class ClassValueScope(blueprint: String)
            extends AbstractValueScope[PuppetDescription[_]]("CLASS", 0, blueprint) {

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

                        val mods = desc.method.getModifiers
                        Modifier.isPrivate(mods) || Modifier.isStatic(mods) || Modifier.isFinal(mods)
                    })
                    .foreach(action)
        })

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

    case class MethodValueScope(blueprint: String, pos: Int)
            extends AbstractValueScope[MethodDescription]("INHERITED_METHODS", pos, blueprint) {

        registerValue("ReturnType" ~> getReturnType)
        registerValue("DefaultReturnType" ~> (_.getDefaultTypeReturnValue))
        registerValue("GenericTypes" ~> getGenericParams)
        registerValue("MethodName" ~> (_.method.getName))
        registerValue("MethodExceptions" ~> getMethodThrows)
        registerValue("MethodID" ~> (_.methodId.toString))
        registerValue("InvokeOnlyResult" ~> (_.getDefaultReturnValue))
        registerValue("ParamsIn" ~> getParametersIn)
        registerValue("ParamsOut" ~> getParametersOut)

        private def getReturnType(desc: MethodDescription): String = {
            val v = TypeVariableTranslator
                    .toScalaDeclaration(desc
                            .method
                            .getGenericReturnType)
            v
        }

        private def getGenericParams(desc: MethodDescription): String = {
            val tParams = desc.method.getTypeParameters
            if (tParams.isEmpty)
                ""
            else
                s"[${TypeVariableTranslator.toScalaDeclaration(tParams)}]"
        }

        private def getMethodThrows(methodDesc: MethodDescription): String = {
            val exceptions = methodDesc.method.getGenericExceptionTypes
            if (exceptions.isEmpty)
                return ""
            exceptions
                    .map(s => toGenericTypeName(s))
                    .mkString("throws ", ", ", "")
        }

        private def getParametersIn(methodDesc: MethodDescription): String = {
            var count = 0
            val sb    = new StringBuilder
            methodDesc
                    .method
                    .getGenericParameterTypes.foreach(typ => {
                val typeName = TypeVariableTranslator.toScalaDeclaration(typ)
                count += 1
                sb
                        .append("arg")
                        .append(count)
                        .append(": ")
                        .append(typeName)
                        .append(", ")
            })
            val v = sb.toString().dropRight(2) //Remove last ", " string.
            v
        }

        private def getParametersOut(methodDesc: MethodDescription): String = {
            val sb = new StringBuilder
            for (i <- 1 to methodDesc.method.getParameterCount) {
                sb.append("arg")
                        .append(i)
                        .append(", ")
            }
            sb.dropRight(2).toString()
        }

        private def toGenericTypeName(typ: Type): String = {
            //The successor name can be the Package for a top level class or a Class File name for an inner class.
            val name             = typ.getTypeName
            val genericTypeBegin = name.indexOf('<')
            if (genericTypeBegin >= 0) {
                val typeOnly = name.take(genericTypeBegin)
                return typeOnly + name.drop(genericTypeBegin).replace('.', '$')
            }
            name
        }
    }
}
