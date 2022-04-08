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

package fr.linkit.macros

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

object ClassStaticAccessorGenerator {

    def newStaticAccessor[T <: AnyRef]: Any = macro newStaticAccessorMacro[T]

    def test[T <: Singleton](x: T): T with Singleton = x

    def newStaticAccessorMacro[T <: AnyRef : c.WeakTypeTag](c: whitebox.Context): c.Expr[Any] = {
        import c.universe._
        //println("Hello, i'm executed at build time !")
        val tpe = implicitly[WeakTypeTag[T]].tpe.companion
        println(s"tpe = ${tpe}")
        println(s"test = ${c.enclosingMethod}")
        val staticMembers      = tpe.members.filter(_.isStatic).filter(_.isMethod)
        //println(s"tpe.members.filter(_.isStatic).filter(_.isMethod) = $staticMembers")
        val generatedClassCode =
            s"""
               |class generatedClass() {
               |                ${
                staticMembers
                    .map(member => s"def ${member.name.encodedName}${member.typeSignature} = ???")
                    .mkString("\n")
            }
               |}""".stripMargin
        //println(s"generatedClassCode = ${generatedClassCode}")
        val quotes             =
            q"""
                ${c.parse(generatedClassCode)}
                val testObject = new generatedClass()
                println("wtf " + testObject)
                testObject
            """
        c.Expr(quotes)
    }

}
