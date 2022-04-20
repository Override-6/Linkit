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

package fr.linkit.engine.test

import org.junit.jupiter.api.Test

import java.lang.reflect.Proxy

class ProxyTests {

    trait ProxiedObject {

        def test(x: String): String
    }

    class ProxiedObjectImpl extends ProxiedObject {

        override def test(x: String): String = {
            x * 2
        }
    }

    @Test
    def makeTest(): Unit = {
        val proxy = Proxy.newPrxyInstance(classOf[ProxiedObject].getClassLoader, Array(classOf[ProxiedObject]), (p, m, args) => {
            println(p, m, args)
            "testlol"
        }).asInstanceOf[ProxiedObject]
        proxy.test("testtestdqsdzq dqd zd qzd zqd ")
    }

}
