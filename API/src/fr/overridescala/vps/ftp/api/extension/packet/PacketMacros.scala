package fr.overridescala.vps.ftp.api.`extension`.packet

import scala.annotation.StaticAnnotation
import scala.reflect.macros.whitebox

object PacketMacros {

    class packetBuilder extends StaticAnnotation {
        scala.language.experimental.macros
        def macroTransform(annottees: Any*): Any = macro macroTransformImpl
    }


    def macroTransformImpl(c: whitebox.Context)(annottees: c.Tree*): c.universe.Tree = {
        import c.universe._

        println(s"annottees = ${annottees}")

        //packet type name    packet constructor parameters
        def genDecompose(tpname: TypeName, paramss: Seq[Seq[ValDef]]): Tree = {

            val parameters = paramss.flatten
            checkParametersSerializable(parameters)

            q"""
               override def decompose(implicit packet: $tpname): Array[Byte] = {

               }
             """
        }

        def checkParametersSerializable(params: Seq[c.universe.ValDef]): Unit = {
            for (valDef <- params) {
                println(valDef.name)
            }
        }

        annottees match {
            case (c@q"$_ class ${tpname: TypeName}[..$_] $_(...${paramss}) extends { ..$_ } with ..$_ { $_ => ..$_ }") :: Nil =>
                q"""
          $c
          object ${tpname.toTermName} {
             object FactoryTest extends PacketFactory[${tpname}] {
             }
          }
          """

            case (c@q"$_ class ${tpname: TypeName}[..$_] $_(...${paramss: Seq[Seq[ValDef]]}) extends { ..$_ } with ..$_ { $_ => ..$_ }") ::
                    q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }" :: Nil =>

                q"""
           $c
           $mods object $tname extends { ..$earlydefns } with ..$parents { $self =>
            ..$body
            ${genDecompose(tpname, paramss)}
            }
          """

            case _ => c.abort(c.enclosingPosition, "Only case classes could be annotated using @buildable")
        }
        q"""
          object FactoryTest extends PacketFtory {

          }
          """
    }


}
