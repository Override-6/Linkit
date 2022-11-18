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

package fr.linkit.api.gnom.persistence.context

trait TypeProfile[T <: AnyRef] {


    val typeClass: Class[_]

    /**
     * Selects a persistor that could deserialize t.
     * the returned persistor is the n-th choice among persistor that could handle t
     *
     * @param t obj to select a valid deserializer
     * @param selectionChoice the n-th choice.
     * */
    def selectPersistor(t: T, selectionChoice: Int): TypePersistor[T]

    def selectPersistor(args: Array[Any]): TypePersistor[T]

    def getPersistences: Array[TypePersistor[T]]
}
