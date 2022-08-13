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

package fr.linkit.engine.internal.language.bhv.interpreter

import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDefUnique, SyncStructureDescription, FieldDescription => SFieldDescription, MethodDescription => SMethodDescription}
import fr.linkit.api.internal.generation.compilation.CompilerCenter
import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.integration.FileIntegratedLambdas
import fr.linkit.engine.internal.language.bhv.interpreter.BehaviorFile.PrimitiveClasses
import fr.linkit.engine.internal.mapping.MappedClassesTree

import java.lang.reflect.Modifier
import scala.util.Try

class BehaviorFile(val ast: BehaviorFileAST, val filePath: String, center: CompilerCenter) {

    val imports = computeImports()
    val lambdas = new FileIntegratedLambdas(center, ast)

    ast.codeBlocks.foreach(_.valueTypes.foreach(findClass))

    private def computeImports(): Seq[PartialFunction[String, Class[_]]] = {
        ast.classImports.map {
            case ClassImport(className, 0) if Try(Class.forName(className)).isSuccess =>
                val array = className.split('.');
                {
                    case name if array.lastOption.contains(name) => Class.forName(className)
                }
            case ClassImport(packageName, 0)                                          =>
                val array = packageName.split('.');
                {
                    case s"$prefix.$name" if array.lastOption.contains(prefix) => Class.forName(packageName + "." + name)
                }
            case ClassImport(pck, 1)                                                  => {
                case name if Try(Class.forName(pck + "." + name)).isSuccess => Class.forName(pck + "." + name)
            }
            case ClassImport(rootPck, 2)                                              => {
                case name if MappedClassesTree.getClass(rootPck, name).isDefined =>
                    MappedClassesTree.getClass(rootPck, name).get
            }
        }
    }

    def findClass(name: String): Class[_] = {
        var arrayIndex = name.indexOf('[')
        if (arrayIndex == -1) arrayIndex = name.length
        val pureName   = name.take(arrayIndex)
        val arrayDepth = (name.length - pureName.length) / 2
        val clazz      = imports
                .find(x => x.isDefinedAt(pureName)).map(_.apply(pureName))
                .orElse(Try(Class.forName(pureName)).toOption)
                .orElse(PrimitiveClasses.get(pureName))
                .orElse(Try(Class.forName("java.lang." + pureName)).toOption)
                .getOrElse {
                    throw new BHVLanguageException(s"Unknown class '$pureName' ${if (!pureName.contains(".")) ", is it imported ?" else ""}")
                }
        lambdas.addImportedClass(clazz)
        (0 until arrayDepth).foldLeft[Class[_]](clazz)((cl, _) => cl.arrayType())
    }

    def formatClassName(clazz: Class[_]): String = {
        var arrayDepth = 0
        var cl         = clazz
        while (cl.isArray) {
            cl = cl.componentType()
            arrayDepth += 1
        }
        val name = cl.getName.replace(".", "_")
        if (arrayDepth == 0) // it's not an array
            name
        else
            name + s"_array_$arrayDepth"
    }

    def getMethodDescFromSignature(kind: DescriptionKind, signature: MethodSignature, classDesc: SyncStructureDescription[_]): SMethodDescription = {
        val name   = signature.methodName
        val params = signature.params.map(param => findClass(param.tpe)).toArray
        if (!classDesc.specs.isInstanceOf[SyncClassDefUnique])
            throw new BHVLanguageException("class description's sync class definition is not 'unique' ")
        val method = {
            try classDesc.specs.mainClass.getDeclaredMethod(name, params: _*)
            catch {
                case _: NoSuchMethodException => throw new BHVLanguageException(s"Unknown method $signature in ${classDesc.specs}")
            }
        }
        val static = Modifier.isStatic(method.getModifiers)
        //checking if method is valid depending on the description context
        kind match {
            case StaticsDescription if !static                            => throw new BHVLanguageException(s"Method '$signature' is not static.")
            case _@(LeveledDescription(_) | RegularDescription) if static => throw new BHVLanguageException(s"Method '$signature' is static. ")
            case _                                                        =>
        }

        val methodID = SMethodDescription.computeID(method)
        classDesc.findMethodDescription(methodID).get
    }

    def getFieldDescFromName(kind: DescriptionKind, name: String, classDesc: SyncStructureDescription[_]): SFieldDescription = {
        val clazz     = classDesc.specs
        val fieldDesc = {
            classDesc.listFields().find(_.javaField.getName == name).getOrElse {
                throw new BHVLanguageException(s"Unknwown field $name in $clazz")
            }
        }
        val field     = fieldDesc.javaField
        val static    = Modifier.isStatic(field.getModifiers)
        kind match {
            case StaticsDescription if !static                            => throw new BHVLanguageException(s"Field $name is not static.")
            case _@(LeveledDescription(_) | RegularDescription) if static => throw new BHVLanguageException(s"Field $name is static. ")
            case _                                                        =>
        }
        fieldDesc
    }
}

object BehaviorFile {

    import java.lang

    private final val PrimitiveClasses = Array(
        Integer.TYPE,
        lang.Byte.TYPE,
        lang.Short.TYPE,
        lang.Long.TYPE,
        lang.Double.TYPE,
        lang.Float.TYPE,
        lang.Boolean.TYPE,
        Character.TYPE
    ).map(cl => (cl.getName, cl)).toMap
}
