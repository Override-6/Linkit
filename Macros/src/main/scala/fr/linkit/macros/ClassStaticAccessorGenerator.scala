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

import java.nio.file.Path
import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

object ClassStaticAccessorGenerator {

    private def sidePrint[T](t: T): T = {
        println(t)
        t
    }

    class property(name: String) extends StaticAnnotation {

        def macroTransform(annottees: Any*): Any = macro propertyImpl

    }

    def propertyImpl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.universe.Tree = {
        import c.universe._

        val defName = c.prefix.tree match {
            case q"new property($n)" => c.eval[String](c.Expr(n))

            case _ => c.abort(c.enclosingPosition, "parameter 'name' is not defined.")
        }

        val inputs = annottees.map(_.tree).toList

        val (valType, valName) = inputs.head match {
            case q"$modifiers var $name: Option[$tpe] = $value" => Tuple2(tpe, name)

            case _ => c.abort(c.enclosingPosition, "the annotated variable should look like '<modifiers> var <name>: <type> = <value>")
        }

        if (valName.equals(defName)) c.abort(c.enclosingPosition, "'name' cannot be equal to the annotated variable's name")

        val valDef = inputs.head

        q"""
         $valDef

         ${generateAccessors(c)(defName, valType, valName)}"""
    }

    def generateAccessors(c: whitebox.Context)(defName: String, valType: c.universe.Tree, valName: c.universe.TermName): c.universe.Tree = {
        import c.universe._

        val getter = TermName(defName)
        val setter = TermName(defName + "_$eq")

        q"""
         def $getter: Option[$valType] = $valName

         def $setter(value: $valType): Unit = $valName = Option(value)"""
    }

    class buildable extends StaticAnnotation {

        def macroTransform(annottees: Any*): Any = macro buildableImpl
    }

    def buildableImpl(c: whitebox.Context)(annottees: c.Tree*): c.universe.Tree = {
        import c.universe._

        def attributes(paramss: Seq[ValDef]): Seq[Tree] = paramss.flatMap {
            case v@q"$modifiers val $name: Option[$tpt] = $rhs" =>
                val newVal = ValDef(Modifiers(Flag.MUTABLE), TermName(s"${name.toString}Opt"), v.tpt, rhs)
                generateAccessors(c)(name.toString, tpt, TermName(s"${name.toString}Opt")).children.prepended(newVal)

            case _: ValDef => Seq.empty
        }

        def isOptional(valDef: ValDef): Boolean = valDef match {
            case q"$modifiers val $name: Option[$tpt] = $rhs" => true

            case _: ValDef => false
        }

        def renameValue(value: ValDef): TermName = value match {

            case opt: ValDef if isOptional(opt) => TermName(s"${opt.name.toString}Opt")

            case required: ValDef => required.name
        }

        def genBuilder(tpname: TypeName, paramss: Seq[Seq[ValDef]]): Tree = {

            val parameters = paramss.flatten
            val constructorParams = parameters.filterNot(isOptional)

            println(parameters)

            q"""
          class Builder(..$constructorParams) extends io.github.iltotore.scalabuilder.Builder[$tpname] {
              ..${attributes(parameters)}

              override def build: $tpname = new $tpname(..${parameters.map(renameValue(_))})
          }
         """
        }

        sidePrint(annottees match {
            case (c@q"$_ class $tpname[..$_] $_(...$paramss) extends { ..$_ } with ..$_ { $_ => ..$_ }") :: Nil =>
                q"""
          $c
          object ${tpname.toTermName} {
            ${genBuilder(tpname, paramss)}
          }
          """

            case (c@q"$_ class $tpname[..$_] $_(...$paramss) extends { ..$_ } with ..$_ { $_ => ..$_ }") ::
                q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }" :: Nil =>
                q"""
           $c
           $mods object $tname extends { ..$earlydefns } with ..$parents { $self =>
            ..$body
            ${genBuilder(tpname, paramss)}
            }
          """

            case _ => c.abort(c.enclosingPosition, "Only case classes could be annotated using @buildable")
        })
    }

}
