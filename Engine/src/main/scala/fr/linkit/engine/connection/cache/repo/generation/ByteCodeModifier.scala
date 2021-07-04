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

package fr.linkit.engine.connection.cache.repo.generation

import fr.linkit.api.connection.cache.repo.generation.GeneratedClassClassLoader
import fr.linkit.engine.connection.cache.repo.generation.ByteCodeModifier._
import fr.linkit.engine.local.utils.NumberSerializer

import java.nio.ByteBuffer
import java.nio.file.Files
import scala.collection.mutable.ListBuffer

/**
 * This class will add a super call to all anonymous functions of a given class file.
 * */
class ByteCodeModifier(className: String, classLoader: GeneratedClassClassLoader, expectedSuperClass: Class[_]) {

    lazy val modifiedClass: Class[_] = {
        val classPath = classLoader.classRootFolder.resolve(className.replace(".", "/") + ".class")
        val bytes     = Files.readAllBytes(classPath)
        val result    = ListBuffer.from(bytes)
        val buff      = ByteBuffer.wrap(bytes)
        buff.position(8)
        println(s"buff.position() = ${buff.position()}")

        val constantPoolCount = buff.getChar: Int
        println(s"constantPoolCount = ${constantPoolCount}")
        for (i <- 1 to constantPoolCount) {
            val flag = buff.get()
            handleModification(i, buff, result)
            makeScan(i, flag, buff)
        }
        classLoader.defineClass(className, result.toArray)
    }

    private def makeScan(i: Int, flag: Int, buff: ByteBuffer): Unit = {

        def printConstant(str: String): Unit = {
            println(s"#$i: ${Flags(flag)} -> $str")
        }

        flag match { //matching flag
            case Utf8                                           => printConstant(decodeString(buff))
            case Class | String | MethodType | Module | Package => printConstant(s"#${buff.getChar: Int}")
            case Integer                                        => printConstant(s"#${buff.getInt}")
            case Float                                          => printConstant(s"#${buff.getFloat}")
            case Double                                         => printConstant(s"#${buff.getDouble}")
            case Long                                           => printConstant(s"#${buff.getLong}")
            case InvokeDynamic | MethodRef |
                 NameAndType | InterfaceMethodRef | FieldRef    => printConstant(s"#${buff.getChar: Int}, #${buff.getChar: Int}")
            case MethodHandle                                   => printConstant(s"#${buff.get}, #${buff.getChar: Int}")
            case flag                                           => println(s"#$i: Unsupported flag: $flag")
        }
    }

    private def handleModification(poolIndex: Int, buff: ByteBuffer, result: ListBuffer[Byte]): Unit = {
        val initialPos = buff.position()
        poolIndex match {
            case ExtendedClass =>
                val oldString = decodeString(buff)
                result.remove(initialPos, oldString.length + 2)

                val newString = expectedSuperClass.getName.replace(".", "/").getBytes
                val array     = NumberSerializer.serializeShort(newString.length.shortValue()) ++ newString
                result.insertAll(initialPos, array)
                val s = decodeString(ByteBuffer.wrap(array))
                println(s"s = ${s}")
            case _             =>
        }
        buff.position(initialPos)
    }

    private def decodeString(buf: ByteBuffer): String = {
        val size     = buf.getChar
        val oldLimit = buf.limit()
        buf.limit(buf.position() + size)
        val sb = new StringBuilder(size + (size >> 1))
        while (buf.hasRemaining) {
            val b = buf.get
            if (b > 0) sb.append(b.toChar)
            else {
                val b2 = buf.get
                if ((b & 0xf0) != 0xe0) sb.append(((b & 0x1F) << 6 | b2 & 0x3F).toChar)
                else {
                    val b3 = buf.get
                    sb.append(((b & 0x0F) << 12 | (b2 & 0x3F) << 6 | b3 & 0x3F).toChar)
                }
            }
        }
        buf.limit(oldLimit)
        sb.toString()
    }

}

object ByteCodeModifier {

    //Types
    val Utf8               = 1
    val Integer            = 3
    val Float              = 4
    val Long               = 5
    val Double             = 6
    val Class              = 7
    val String             = 8
    val FieldRef           = 9
    val MethodRef          = 10
    val InterfaceMethodRef = 11
    val NameAndType        = 12
    val MethodHandle       = 15
    val MethodType         = 16
    val InvokeDynamic      = 18
    val Module             = 19
    val Package            = 20

    val Flags: Array[String] = Array(
        null, "Utf8", null, "Integer", "Float",
        "Long", "Double", "Class", "String",
        "FieldRef", "MethodRef", "InterfaceMethodRef",
        "NameAndType", null, null, "MethodHandle",
        "MethodType", null, "InvokeDynamic", "Module", "Package"
    )

    //Constant Indexes that must be modified
    val ExtendedClass = 4
}
