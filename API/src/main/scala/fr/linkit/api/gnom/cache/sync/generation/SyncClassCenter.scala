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

package fr.linkit.api.gnom.cache.sync.generation

import fr.linkit.api.gnom.cache.sync.SynchronizedObject
import fr.linkit.api.gnom.cache.sync.contract.description.{SyncClassDef, SyncStructureDescription}

/**
 * This class generates a class that extends
 * */
trait SyncClassCenter {

    def getSyncClass[S <: AnyRef](clazz: SyncClassDef): Class[S with SynchronizedObject[S]]

    def getSyncClassFromDesc[S <: AnyRef](desc: SyncStructureDescription[S]): Class[S with SynchronizedObject[S]]

    def preGenerateClasses(classes: Seq[SyncClassDef]): Unit


    def isClassGenerated(classDef: SyncClassDef): Boolean

}
