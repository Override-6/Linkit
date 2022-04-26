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

package fr.linkit.engine.internal.mapping

import fr.linkit.api.gnom.network.Engine

import scala.collection.mutable

class RemoteClassMappings(val ownerId: String) extends ClassMappingsListener {

    //a set of all class codes present on the ClassMappings of the owner engine.
    private final val mappedClasses = mutable.HashSet.empty[Int] ++ ClassMappings.classCodes
    ClassMappings.addListener(this)

    //called on the owner engine
    def addClassToMap(className: String): Unit = ClassMappings.putClass(className, ClassLoader.getSystemClassLoader)

    //called by the other engines
    def isClassCodeMapped(classCode: Int): Boolean = mappedClasses.contains(classCode)

    //called by the owner engine to update other RemoteClassMappings objects.
    def onClassMapped(classCode: Int): Unit = mappedClasses += classCode
}
