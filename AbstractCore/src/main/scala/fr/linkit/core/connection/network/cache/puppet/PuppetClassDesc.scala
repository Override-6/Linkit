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

package fr.linkit.core.connection.network.cache.puppet

import fr.linkit.core.connection.network.cache.puppet.generation.InvalidPuppetDefException
import org.jetbrains.annotations.Nullable

import java.lang.annotation.Annotation
import java.lang.reflect._

class PuppetClassDesc private(val puppetClass: Class[_],
                              val puppetConstructor: Constructor[_],
                              val classAnnotation: SharedObject) {

    def isAutoFlush: Boolean = classAnnotation.autoFlush()

    def getSharedField(name: String): Option[Field] = {
        prepare(puppetClass.getDeclaredField(name))
    }

    def getSharedMethod(name: String, args: Seq[Class[_]]): Option[Method] = {
        try {
            Option(puppetClass.getDeclaredMethod(name, args: _*))
        } catch {
            case _: NoSuchMethodException =>
                puppetClass.getDeclaredMethods
                        .filter(_.getName == name)
                        .find(method => {
                            method.getParameterCount == args.size && isShared(method) &&
                                    method.getParameterTypes
                                            .zip(args)
                                            .forall {
                                                case (parameterType, argType) => parameterType.isAssignableFrom(argType)
                                            }
                        })
        }
    }

    def foreachSharedFields(action: Field => Unit): Unit = {
        puppetClass.getDeclaredFields
                .filter(isShared)
                .tapEach(_.setAccessible(true))
                .foreach(action)
    }

    //FIXME methods are only taken from the first super class, they must be taken until Object's class.
    def foreachSharedMethods(action: Method => Unit): Unit = {
        puppetClass.getDeclaredMethods
                .filterNot(member => Modifier.isFinal(member.getModifiers) || Modifier.isStatic(member.getModifiers))
                .filter(isShared)
                .tapEach(_.setAccessible(true))
                .foreach(action)
    }

    private def prepare[M <: AccessibleObject with Member](@Nullable member: M): Option[M] = {
        val opt = Option(member).filter(isShared)
        opt.tapEach(_.setAccessible(true))
        opt
    }

    private def isShared(member: AccessibleObject): Boolean = {
        member.isAnnotationPresent(classOf[Shared]) ||
                (classAnnotation.shareAllMethods() && !member.isAnnotationPresent(classOf[Hidden]))
    }

}

object PuppetClassDesc {

    val DefaultAnnotation: SharedObject = new SharedObject {
        override def autoFlush(): Boolean = true

        override def shareAllMethods(): Boolean = true

        override def annotationType(): Class[_ <: Annotation] = getClass
    }

    def ofRef(anyRef: Serializable): PuppetClassDesc = {
        ofClass(anyRef.getClass)
    }

    def ofClass(clazz: Class[_]): PuppetClassDesc = {

        val annotation = Option(clazz.getAnnotation(classOf[SharedObject])).getOrElse(DefaultAnnotation)

        val simpleName        = clazz.getSimpleName
        val puppetConstructor = clazz.getDeclaredConstructors
                .find(_.getParameterTypes sameElements scala.Array(clazz))
                .getOrElse(throw new InvalidPuppetDefException(
                    s"""For puppet class $clazz
                       |This puppet must contain an accessible constructor '+protected $simpleName($simpleName other)' in order to be extended by a generated class.
                       | If you are not the maintainer of this class, you can simply extend the class, define the appointed constructor and give the implementation
                       | to the puppet generator.
                       |""".stripMargin
                ))

        new PuppetClassDesc(clazz, puppetConstructor, annotation)
    }
}
