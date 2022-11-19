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

package fr.linkit.api.gnom.cache.sync.invocation.local

import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.gnom.network.tag.NameTag
import org.jetbrains.annotations.Nullable

/**
 * The Chip is a class that controls an object of type [[S]]
 * The controlled object is then considered as chipped and its chip
 * can invoke any of its methods and set his fields values.
 * */
trait Chip[S] {

    /**
     * Calls method pointed by methodID parameter.
     * The return value of the invocated method is supplied by the onResult parameter.
     * The method can return immediatelly, or after onResult is called depending on the method's contract.
     * @param methodID the method identifier that must be called
     * @param params the parameters for the method call
     * @param caller the engine that is calling the method
     * @see [[fr.linkit.api.gnom.cache.sync.contract.description.SyncStructureDescription]]
     * */
    def callMethod(methodID: Int, params: Array[Any], callerNT: NameTag)(onException: Throwable => Unit, onResult: Any => Unit): Unit
    /**
     * this method will transfer to the chipped object every fields
     * values of the given object.
     * @param obj the obj in where fields values will be pasted to the chipped object.
     * */
    def updateObject(obj: S): Unit
}
