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

package fr.linkit.api.gnom.cache.sync.invokation.local

/**
 * The Chip is a class that controls an object of type [[S]]
 * The controlled object is then considered as chipped and its chip
 * can invoke any of its methods and set his fields values.
 * */
trait Chip[S] {

    /**
     * this method will transfer to the chipped object every fields
     * values of the given object.
     * @param obj the obj in where fields values will be pasted to the chipped object.
     * */
    def updateObject(obj: S): Unit

    /**
     * Invokes the method of the chipped object.
     * The method is determined by the methodID integer.
     * @param methodID the method identifier that must be called
     * @param params the parameters for the method call
     * @see [[fr.linkit.api.gnom.cache.sync.description.SyncObjectSuperclassDescription]]
     * */
    def callMethod(methodID: Int, params: Array[Any], origin: String): Any
}
