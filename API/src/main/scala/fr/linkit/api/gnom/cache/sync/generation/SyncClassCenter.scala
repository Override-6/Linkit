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

package fr.linkit.api.gnom.cache.sync.generation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassRepresentation, SyncStructureDescription}

import scala.reflect.runtime.universe
import scala.reflect.{ClassTag, classTag}

/**
 * This class generates a class that extends
 * */
trait SyncClassCenter {

    def getSyncClass[S <: AnyRef](clazz: SyncClassRepresentation): Class[S with SynchronizedObject[S]]

    def getSyncClassFromDesc[S<: AnyRef](desc: SyncStructureDescription[S]): Class[S with SynchronizedObject[S]]

    def getSyncClassFromTpe[S<: AnyRef: universe.TypeTag : ClassTag]: Class[S with SynchronizedObject[S]] = getSyncClass[S](SyncClassRepresentation(classTag[S].runtimeClass))

    def preGenerateClasses(classes: Seq[SyncClassRepresentation]): Unit

    def isClassGenerated[T<: AnyRef: ClassTag]: Boolean = isClassGenerated(SyncClassRepresentation(classTag[T].runtimeClass))

    def isClassGenerated(clazz: SyncClassRepresentation): Boolean

}
