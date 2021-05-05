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

package fr.linkit.api.connection.network.cache.repo.generation

import fr.linkit.api.connection.network.cache.repo.{PuppetDescription, PuppetWrapper}

import scala.reflect.{ClassTag, classTag}

/**
 * This class generates a class that extends
 * */
trait PuppetWrapperGenerator {

    def getClass[S <: Serializable](clazz: Class[S]): Class[S with PuppetWrapper[S]]

    def preGenerateClasses[S <: Serializable](classes: Class[S]*): Unit

    def preGenerateClasses[S <: Serializable](blueprints: PuppetDescription[S]*): Unit

    def isClassGenerated[T <: Serializable : ClassTag]: Boolean = isClassGenerated(classTag[T].runtimeClass)

    def isClassGenerated(clazz: Class[_]): Boolean

    def forgetClass(clazz: Class[_]): Unit

}
