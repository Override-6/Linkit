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

package fr.linkit.engine.gnom.cache.sync.generation

import fr.linkit.api.gnom.cache.sync.contract.description.{MethodDescription, SyncClassDef, SyncClassDefMultiple, SyncClassDefUnique, SyncStructureDescription}
import fr.linkit.api.gnom.cache.sync.generation.GeneratedClassLoader
import fr.linkit.api.gnom.cache.sync.invocation.local.AbstractMethodInvocationException
import fr.linkit.api.gnom.cache.sync.{ConnectedObjectReference, SynchronizedObject}
import fr.linkit.api.gnom.referencing.{NetworkObject, NetworkObjectReference}
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncObjectDescription
import SyncClassBlueprint.unsupportedMethodFilter
import SyncClassRectifier.{JavaKeywords, SuperMethodModifiers, getMethodDescriptor}
import javassist._
import javassist.bytecode.annotation._
import javassist.bytecode.{AnnotationsAttribute, MethodInfo}

import java.lang.reflect.{Constructor, Method, Modifier}
import scala.annotation.switch
import scala.collection.immutable.HashSet
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class SyncClassRectifier(desc: SyncObjectDescription[_],
                         syncClassName: String,
                         classLoader: GeneratedClassLoader,
                         syncClassDef: SyncClassDef) {

    private val mainClass = syncClassDef.mainClass
    private val interfaces = syncClassDef match {
        case _: SyncClassDefUnique     => Array[Class[_]]()
        case multiple: SyncClassDefMultiple => multiple.interfaces
    }
    private val pool       = ClassPool.getDefault
    pool.appendClassPath(new LoaderClassPath(classLoader))
    private val ctClass = pool.get(syncClassName)
    ctClass.defrost()
    ctClass.stopPruning(true)

    applyClassDef()
    fixAllMethods()
    addAllConstructors()
    fixNetworkObjectInherance()

    lazy val rectifiedClass: (Array[Byte], Class[SynchronizedObject[_]]) = {
        val bc = ctClass.toBytecode

        (bc, classLoader.defineClass(bc, ctClass.getName).asInstanceOf[Class[SynchronizedObject[_]]])
    }

    private def applyClassDef(): Unit = {
        def extendClass(clazz: Class[_]): Unit = {
            val ctClass0 = pool.get(clazz.getName)
            if (clazz.isInterface)
                ctClass.addInterface(ctClass0)
            else if (ctClass.getSuperclass.getName == "java.lang.Object")
                ctClass.setSuperclass(ctClass0)
            else throw new UnsupportedOperationException(s"Could not set super class ${ctClass0.getName}, generated class already extends ${ctClass.getSuperclass.getName}")
        }

        extendClass(mainClass)
        syncClassDef match {
            case multiple: SyncClassDefMultiple => multiple.interfaces.foreach(extendClass)
            case _                              =>
        }
    }

    private def fixNetworkObjectInherance(): Unit = {
        if (!classOf[NetworkObject[_]].isAssignableFrom(mainClass))
            return

        def slashDot(cl: Class[_]): String = cl.getName.replace(".", "/")

        // Removes a potential 'reference' method that overrides the actual superClass's reference method and returns the wrong reference object
        val met = ctClass.getDeclaredMethods
                .find(m => m.getName == "reference" && m.getReturnType.getName != classOf[ConnectedObjectReference].getName)
                .get
        ctClass.removeMethod(met)
        addMethod("reference", s"()L${slashDot(classOf[NetworkObjectReference])};")
                .setBody("return location();")
    }

    private def addMethod(name: String, signature: String): CtMethod = {
        val info   = new MethodInfo(ctClass.getClassFile.getConstPool, name, signature)
        val method = CtMethod.make(info, ctClass)
        method.setModifiers(Modifier.PUBLIC)
        ctClass.addMethod(method)
        method
    }

    private def addAllConstructors(): Unit = {
        mainClass.getDeclaredConstructors.foreach(constructor => if (!Modifier.isPrivate(constructor.getModifiers)) {
            if (constructor.getParameterCount > 0) {
                val const  = addConstructor(constructor)
                val params = constructor.getParameterTypes
                const.setBody(
                    s"""
                       |super(${params.indices.map(i => s"$$${i + 1}").mkString(",")});
                       |""".stripMargin)
            }
        })
    }

    private def addConstructor(constructor: Constructor[_]): CtConstructor = {
        val params        = constructor.getParameterTypes
        val ctConstructor = new CtConstructor(Array.empty, ctClass)
        val constPool     = ctClass.getClassFile.getConstPool
        val annAttribute  = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
        constructor.getAnnotations.foreach(ann => {
            val annType    = ann.annotationType()
            val annotation = new Annotation(annType.getName, constPool)
            addAllMembers(ann, annotation)
            annAttribute.addAnnotation(annotation)
        })
        ctConstructor.getMethodInfo.addAttribute(annAttribute)
        params.foreach(param => ctConstructor.addParameter(pool.get(param.getName)))
        ctClass.addConstructor(ctConstructor)
        ctConstructor
    }

    private def cast[X](y: Any): X = y.asInstanceOf[X]

    private def addAllMembers(ann: java.lang.annotation.Annotation, annotation: Annotation): Unit = {
        val constPool = ctClass.getClassFile.getConstPool
        ann.annotationType().getDeclaredMethods.foreach { method =>
            val returnType  = method.getReturnType
            val value       = method.invoke(ann)
            val memberValue = (returnType.getName: @switch) match {
                case "int"              => new IntegerMemberValue(constPool, cast(value))
                case "byte"             => new ByteMemberValue(cast(value), constPool)
                case "double"           => new DoubleMemberValue(cast(value), constPool)
                case "long"             => new LongMemberValue(cast(value), constPool)
                case "short"            => new ShortMemberValue(cast(value), constPool)
                case "char"             => new CharMemberValue(cast(value), constPool)
                case "float"            => new FloatMemberValue(cast(value), constPool)
                case "boolean"          => new BooleanMemberValue(cast[Boolean](value), constPool)
                case "java.lang.String" => new StringMemberValue(cast[String](value), constPool)
                case "java.lang.Class"  => new ClassMemberValue(cast[Class[_]](value).getName, constPool)
            }
            annotation.addMemberValue(method.getName, memberValue)
        }
    }

    private def fixAllMethods(): Unit = {
        implicit def extractModifiers(m: Method): Int = m.getModifiers

        val methodDescs      = desc.listOverrideableMethods()
                .toSeq
                .filterNot(unsupportedMethodFilter)
                .distinctBy(x => (x.javaMethod.getParameterTypes.toList, x.getName))
        val superDescriptors = ListBuffer.empty[String]
        val methodNames      = ListBuffer.empty[String]

        def fixMethod(desc: MethodDescription): Unit = {
            val javaMethod = desc.javaMethod
            val name       = javaMethod.getName
            if (JavaKeywords.contains(name))
                throw new UnsupportedOperationException(s"Can't generate method rectification for method $javaMethod: this method's name is a java token.")
            val superfunName = s"super$$$name$$"
            val superfunInfo = new MethodInfo(ctClass.getClassFile.getConstPool, superfunName, getMethodDescriptor(javaMethod))
            if (superDescriptors.contains(superfunName + superfunInfo.getDescriptor))
                return
            val anonfun  = getAnonFun(desc)
            val superfun = CtMethod.make(superfunInfo, ctClass)
            superfun.setModifiers(SuperMethodModifiers)

            ctClass.addMethod(superfun)
            val source = getSuperFunBody(javaMethod)
            superfun.setBody(source)
            val body = getAnonFunBody(javaMethod, superfun)
            anonfun.setBody(body)

            superDescriptors += superfunName + superfunInfo.getDescriptor
            methodNames += javaMethod.getName
        }

        for (desc <- methodDescs) {
            fixMethod(desc)
        }
    }


    private def getAnonFun(desc: MethodDescription): CtMethod = {
        val javaMethod       = desc.javaMethod
        val methodReturnType = javaMethod.getReturnType
        val methodDesc       = getMethodDescriptor(javaMethod)
        val anonFunPrefix    = s"$$anonfun$$${javaMethod.getName}$$"
        val filtered         = ctClass.getDeclaredMethods
                .filter(_.getName.startsWith(anonFunPrefix))
                .filterNot(_.getName.endsWith("adapted"))
        val method           = filtered
                .find { x =>
                    val params = x.getParameterTypes.drop(1).dropRight(1)
                    val desc   = getMethodDescriptor(params, methodReturnType)
                    desc == methodDesc
                }
                .getOrElse {
                    throw new NoSuchElementException(s"Could not find anonymous function '$anonFunPrefix'")
                }
        method
    }

    private def getAnonFunBody(javaMethod: Method, superFun: CtMethod): String = {

        val params     = javaMethod.getParameterTypes
        val arrayIndex = params.length + 2
        val str        = s"$$1.${superFun.getName}(${
            (0 until javaMethod.getParameterCount).map(i => {
                val clazz = params(i)
                s"(${clazz.getTypeName}) ${getWrapperFor(clazz, s"$$$arrayIndex[$i]", true)}"
            }).mkString(",")
        })"

        if (javaMethod.getReturnType == Void.TYPE)
            s"{$str; return null;}"
        else {
            s"{return ${getWrapperFor(superFun.getReturnType, str)};}"
        }
    }

    private def getWrapperFor(returnType: CtClass, str: String): String = {
        if (returnType.isPrimitive) {
            var wrapperName = returnType.getName.head.toUpper + returnType.getName.drop(1)
            if (wrapperName == "Int")
                wrapperName = "Integer"
            s"$wrapperName.valueOf($str)"
        }
        else str
    }

    private def getWrapperFor(returnType: Class[_], str: String, unwrap: Boolean = false): String = {
        if (returnType.isPrimitive) {
            var wrapperName = returnType.getName.head.toUpper + returnType.getName.drop(1)
            if (wrapperName == "Int")
                wrapperName = "Integer"
            if (unwrap) {
                val methodName = returnType.getName + "Value"
                s"(($wrapperName) $str).$methodName()"
            } else
                s"$wrapperName.valueOf($str)"
        }
        else str
    }

    private def getSuperFunBody(javaMethod: Method): String = {
        if (Modifier.isAbstract(javaMethod.getModifiers) && !Modifier.isNative(javaMethod.getModifiers)) {
            s"""{
               |Throwable x = new ${classOf[AbstractMethodInvocationException].getName}(\"Attempted to call an abstract method on this object. (${javaMethod.getName} in ${mainClass} is abstract.)\", null);
               |throw x;
               |}
               |""".stripMargin
        } else {
            val str = {
                val declaringClass = javaMethod.getDeclaringClass
                val enclosing      = {
                    if (declaringClass.isInterface) {
                        if (declaringClass.isAssignableFrom(mainClass)) {
                            if (mainClass.isInterface) s"${mainClass.getName}." else ""
                        }
                        else interfaces.find(declaringClass.isAssignableFrom).get.getName + "."
                    } else ""
                }

                s"${enclosing}super.${javaMethod.getName}(${(1 to javaMethod.getParameterCount).map(i => s"$$$i").mkString(",")});".stripMargin
            }
            if (javaMethod.getReturnType == Void.TYPE) str
            else s"return $str"
        }
    }

}

object SyncClassRectifier {

    //used AccessFlags that are not in the java's reflection public api
    val Access_Synthetic          = 0x00001000
    val SuperMethodModifiers: Int = Modifier.PRIVATE + Access_Synthetic

    import java.{lang => l}
    
    private final val StringToPrimitiveID =
        Map(
            "int" -> "I",
            "double" -> "D",
            "float" -> "F",
            "char" -> "C",
            "boolean" -> "Z",
            "long" -> "J",
            "void" -> "V",
            "short" -> "S",
            "byte" -> "B"
        )

    final val JavaKeywords = HashSet("default", "if", "for", "abstract", "assert", "boolean", "int", "float",
        "byte", "case", "switch", "break", "continue", "return", "try", "catch", "var", "goto", "const", "do", "while",
        "else", "enum", "interface", "final", "record", "sealed", "implements", "finally", "float", "double", "char",
        "import", "instanceof", "long", "native", "new", "package", "private", "public", "protected", "isinstance",
        "short", "static", "strictfp", "super", "this", "synchronized", "throw", "throws", "transient", "void", "volatile")

    def getMethodDescriptor(method: Method): String = {
        getMethodDescriptor(method.getParameterTypes, method.getReturnType)
    }

    def getMethodDescriptor(params: Array[Class[_]], returnType: Class[_]): String = {

        val sb = new mutable.StringBuilder("(")
        params.foreach { clazz =>
            sb.append(typeStringClass(clazz))
        }
        sb.append(')')
                .append(typeStringClass(returnType))
        sb.toString()
    }

    def getMethodDescriptor(params: Array[CtClass], returnType: Class[_]): String = {

        val sb = new mutable.StringBuilder("(")
        params.foreach { clazz =>
            sb.append(typeStringCtClass(clazz))
        }
        sb.append(')')
                .append(typeStringClass(returnType))
        sb.toString()
    }

    def typeStringCtClass(clazz: CtClass): String = {
        var cl      = clazz
        val finalSB = new mutable.StringBuilder
        while (cl.isArray) {
            finalSB.append("[")
            cl = clazz.getComponentType
        }
        val jvmTpe = StringToPrimitiveID.getOrElse(cl.getName, {
            val objSB = new mutable.StringBuilder()
            objSB.append("L")
                    .append(cl.getName.replace(".", "/"))
                    .append(";")
            objSB.toString()
        })
        finalSB.append(jvmTpe)
        finalSB.toString()
    }

    def typeStringClass(clazz: Class[_]): String = {
        if (clazz == Void.TYPE)
            return "V"
        val arrayString = java.lang.reflect.Array.newInstance(clazz, 0).toString
        arrayString.slice(1, arrayString.indexOf('@')).replace(".", "/")
    }

}
