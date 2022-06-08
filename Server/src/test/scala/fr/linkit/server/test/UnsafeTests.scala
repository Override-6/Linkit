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

package fr.linkit.server.test

import fr.linkit.engine.gnom.persistence.serializor.ConstantProtocol
import fr.linkit.engine.internal.utils.ScalaUtils
import org.junit.jupiter.api.Test

import java.lang.reflect.Modifier

class UnsafeTests {
    //ServerLauncher.launch()
    
    //private val U = //ScalaUtils.findInternalUnsafe()
    private val U = jdk.internal.misc.Unsafe.getUnsafe
    
    @Test
    def test(): Unit = {
        val obj             = new MyObject("samuel")
        val impostor        = new MyObject("impostor")
        val wrapper         = new MyObjectWrapper(obj)
        val wrapperImpostor = new MyObjectWrapper(impostor)
        val objSize = objectSize(obj)
    
        val field = wrapper.getClass.getDeclaredField("value")
        field.setAccessible(true)
        val pointer  = U.getAddress(wrapper, U.objectFieldOffset(field))
        val pointer2 = U.getAddress(wrapperImpostor, U.objectFieldOffset(field))
        pasteAll(pointer, pointer2, objSize)
        print(pointer)
    }
    
    //copy heap memory from addr1 to addr2
    private def pasteAll(addr1: Long, addr2: Long, size: Int): Unit = {
        val refCount = size / U.addressSize()
        var refIdx   = 0
        while (refIdx < refCount) {
            val i = U.getAddress(addr1 + refIdx * U.addressSize())
            U.putAddress(addr2 + refIdx * U.addressSize(), i)
            refIdx += U.addressSize()
        }
    }
    
    //estimate the object's length in heap memory
    private def objectSize(obj: Object): Int = {
        ScalaUtils.retrieveAllFields(obj.getClass)
                .filterNot(f => Modifier.isStatic(f.getModifiers))
                .map(f => U.arrayIndexScale(f.getType.arrayType()))
                .sum + U.addressSize() //add the class ref size
    }
    
    private class MyObject(val name: String)
    
    private class MyObjectWrapper(val value: MyObject)
    
}