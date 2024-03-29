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

package linkit.base.compilation.cbp

class BlueprintValueSupplier[A](name: String, bp: String, val supplier: A => String) extends AbstractBlueprintValue[A](name, bp) {

    override def getValue(a: A): String = supplier(a)

}

object BlueprintValueSupplier {

    def apply[A](tuple: (String, A => String))(implicit blueprint: String): BlueprintValueSupplier[A] = {
        new BlueprintValueSupplier(tuple._1, blueprint, tuple._2)
    }

    def getAll[A](tuple: (String, A => String)*)(implicit blueprint: String): Seq[BlueprintValueSupplier[A]] = {
        tuple.map(apply)
    }
}
