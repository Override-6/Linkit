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

import fr.linkit.core.local.utils.ScalaUtils

import java.lang.reflect.Modifier

class Puppeteer[S <: Serializable] private(puppet: S) {

    val accessor: PuppetAccessor = PuppetAccessor.ofRef(puppet)

    val autoFlush: Boolean = accessor.isAutoFlush

    def updateField(fieldName: String, value: Any): Unit = {
        accessor.getSharedField(fieldName)
                .fold()(ScalaUtils.setValue(_, puppet, value))
    }

    def updateAllFields(obj: Serializable): Unit = {
        accessor.foreachSharedFields(field => {
            val value = field.get(obj)
            ScalaUtils.setValue(field, puppet, value)
        })
    }

    def canCallMethod(methodName: String): Boolean = accessor.getSharedMethod(methodName).isDefined

    def callMethod(methodName: String, params: Serializable*): Serializable = {
        if (!canCallMethod(methodName))
            throw new PuppetException(s"Attempted to invoke cached method '$methodName'")

        val method = accessor.getSharedMethod(methodName).get
        method.invoke(methodName, params)
                .asInstanceOf[Serializable]
    }

}

object Puppeteer {

    def apply[S <: Serializable](puppet: S): Puppeteer[S] = {
        if (puppet == null)
            throw new NullPointerException("puppet is null !")
        val clazz = puppet.getClass
        if (!clazz.isAnnotationPresent(classOf[SharedObject]))
            throw new IllegalPuppetException(s"$clazz: This puppet's class must be annotated with @SharedObject in order to be manipulated by a chip.")

        if (Modifier.isFinal(clazz.getModifiers))
            throw new IllegalPuppetException("Puppet can't be final.")

        new Puppeteer[S](puppet)
    }

}
