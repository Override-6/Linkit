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

package fr.linkit.engine.connection.cache.repo.generation.rectifier

import fr.linkit.api.connection.cache.repo.generation.GeneratedClassClassLoader
import fr.linkit.engine.connection.cache.repo.generation.rectifier.ByteCodeRectifier._
import fr.linkit.engine.local.utils.ArrayByteInserter
import fr.linkit.engine.local.utils.NumberSerializer.{serializeShort => serialShort}

import java.lang.reflect.{Method, Modifier}
import java.nio.ByteBuffer
import java.nio.file.{Files, Path}
import java.util
import scala.collection.mutable.ListBuffer

/**
 * This class will add a super call to all anonymous functions of a given class file.
 * */
class ByteCodeRectifier(className: String, classLoader: GeneratedClassClassLoader, expectedSuperClass: Class[_]) {

    import ByteCodeRectifier.shortConversion

    private val classPath = classLoader.classRootFolder.resolve(className.replace(".", "/") + ".class")
    private val bytes     = Files.readAllBytes(classPath)
    private val inserter  = new ArrayByteInserter(bytes)
    private val buff      = ByteBuffer.wrap(bytes)

    buff.position(8)
    private var constantPoolCount = (buff.getChar - 1).toShort
    println(s"constantPoolCount = ${constantPoolCount}")

    private var constantPoolPositions = new Array[Int](constantPoolCount)
    private val bufferPoolStartPos    = buff.position()

    iterateConstantPool(buff)
    private val bufferPoolEndPos          = buff.position()
    private val thisClassIndex            = buff.getChar(bufferPoolEndPos + 2): Int
    private val superClassIndex           = buff.getChar(bufferPoolEndPos + 4): Int //skips three shorts, access_flag, this_class and super_class
    private var codeAttributeIndex: Short = -1 //will be set into addValuesInConstantPool method
    private val methods                   = ListBuffer.empty[MethodBytecode] // will be fill in the addValuesInConstantPool method

    handleSuperClassModification()
    addValuesInConstantPool()
    addSuperMethodsInMethodPool()

    iterateConstantPool(ByteBuffer.wrap(inserter.getResult.drop(bufferPoolStartPos)))

    lazy val rectifiedClass: Class[_] = {
        val bytes = inserter.getResult
        Files.write(Path.of("C:\\Users\\maxim\\Desktop\\BCScanning\\PuppetScalaClassModifiedBC.class"), bytes)
        classLoader.defineClass(className, bytes)
    }

    private def iterateConstantPool(buff: ByteBuffer): Unit = {
        for (i <- 1 until constantPoolCount) {
            val flag = buff.get()
            makeScan(i, flag, buff)
        }
    }

    private def makeScan(i: Int, flag: Int, buff: ByteBuffer): Unit = {

        def printConstant(str: String): Unit = {
            println(s"#$i: ${Flags(flag)} -> $str")
        }

        constantPoolPositions(i) = buff.position()

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

    private def handleSuperClassModification(): Unit = {
        val initialPos          = buff.position()
        val classNameIndex: Int = buff.getChar(constantPoolPositions(superClassIndex))
        val classNamePos        = constantPoolPositions(classNameIndex)
        buff.position(classNamePos)
        decodeString(buff)
        inserter.deleteBlock(classNamePos, buff.position() - classNamePos)

        val newString = expectedSuperClass.getName.replace(".", "/").getBytes("UTF-8")
        val array     = asUtfValue(newString)
        inserter.insertBytes(classNamePos, array)
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

    private def generateSuperMethodSignature(method: Method): String = {

        def typeString(clazz: Class[_]): String = {
            if (clazz == Void.TYPE)
                return "V"
            val arrayString = java.lang.reflect.Array.newInstance(clazz, 0).toString
            arrayString.slice(1, arrayString.indexOf('@')).replace(".", "/")
        }

        val sb = new StringBuilder("(")
        method.getParameterTypes.foreach { clazz =>
            sb.append(typeString(clazz))
        }
        sb.append(')')
                .append(typeString(method.getReturnType))
        sb.toString()
    }

    /*
     * Will add all super methods constants (name, signature, NameAndType and MethodRef)
     * and also add some needed constants such as the 'Code' attribute name
     */
    private def addValuesInConstantPool(): Unit = {
        val javaMethods = expectedSuperClass.getDeclaredMethods.filter(m => Modifier.isPublic(m.getModifiers))
        constantPoolCount += (4 * javaMethods.length) + 1 //+1 for the attribute
        inserter.deleteBlock(8, 2)
        inserter.insertBytes(8, serialShort(constantPoolCount))
        constantPoolPositions = util.Arrays.copyOf(constantPoolPositions, constantPoolCount + 2)
        var currentNameIndex = constantPoolCount
        for (javaMethod <- javaMethods) {
            currentNameIndex -= 4
            val name      = s"super$$${javaMethod.getName}"
            val signature = generateSuperMethodSignature(javaMethod)
            inserter.insertBytes(bufferPoolEndPos, Array(MethodRef) ++ serialShort(thisClassIndex.toShort) ++ serialShort((currentNameIndex + 2).toShort))
            inserter.insertBytes(bufferPoolEndPos, Array(NameAndType) ++ serialShort(currentNameIndex) ++ serialShort((currentNameIndex + 1).toShort))
            inserter.insertBytes(bufferPoolEndPos, Array(Utf8, asUtfValue(signature.getBytes): _*))
            inserter.insertBytes(bufferPoolEndPos, Array(Utf8, asUtfValue(name.getBytes): _*))
            val method = MethodBytecode(javaMethod, currentNameIndex, currentNameIndex + 1, currentNameIndex + 2, currentNameIndex + 4)
            methods += method
        }
        inserter.insertBytes(bufferPoolEndPos, Array(Utf8) ++ asUtfValue("Code".getBytes()))
        codeAttributeIndex = constantPoolCount
    }

    private def addSuperMethodsInMethodPool(): Unit = {
        gotoMethodPool()
        val pos         = buff.position()
        val methodCount = buff.getChar: Int
        inserter.deleteBlock(pos - 2, 2)
        inserter.insertBytes(pos - 2, serialShort(methodCount + methods.length))
        //buffSkip(2) //skip the method pool count number
        for (method <- methods) {
            val bytes: Array[Array[Byte]] = Array(
                serialShort((Modifier.PRIVATE + Access_Synthetic).toShort),
                serialShort(method.nameIdx),
                serialShort(method.signatureIdx), //actually descriptor_index, but i decided to keep the "signature" word in order to don't be lost in my mind
                one, //Only one attribute: the Code attribute.
                //Entering in the attributes array.
                //Creating the only attribute of the method: named 'Code'
            ) ++ generateMethodCodeAttribute(method)
            //here, the iteration is reversed because the insertBytes will have for effect to reverse the order of
            //inserted bytes, so reversing the byte insertion order will counterbalance the reversion
            for (i <- bytes.indices.reverse) {
                inserter.insertBytes(pos, bytes(i))
            }
        }
    }

    private def generateMethodCodeAttribute(method: MethodBytecode): Array[Array[Byte]] = {
        val attributeNameIndex = serialShort(codeAttributeIndex)
        val byteCode           = generateMethodByteCode(method)
        val body               = Array[Array[Byte]](
            one, //max_stack = 1
            serialShort((1 + method.javaMethod.getParameterCount).toShort), //max_locals (+1 because we need to keep the actual instance for the method invocation)
            serialShort(byteCode.length.toShort), //code_length
            byteCode, //code
            zero, //exception_table_length (no exception to handle)
            zero //attributes_count (no attributes to add in the Code attribute)
        )
        Array(
            attributeNameIndex,
            serialShort(body.length.toShort)
        ) ++ body
    }

    private def generateMethodByteCode(method: MethodBytecode): Array[Byte] = {
        Array()
    }

    private def gotoMethodPool(): Unit = {
        //Jumping over inherited interfaces information
        buff.position(bufferPoolEndPos + 6) //skip access_flags, this_class, and super_class
        val interfaceCount = buff.getChar(): Int
        buffSkip(interfaceCount * 2)

        //Jumping over fields information
        val fieldCount = buff.getChar(): Int
        for (_ <- 1 to fieldCount) {
            buffSkip(6) //skip access_flag, name_index and descriptor_index
            val attributesCounts = buff.getChar(): Int
            //skips all attributes_info
            for (_ <- 1 to attributesCounts) {
                buffSkip(2) //skip the NameIndex
                buffSkip(buff.getInt) //skip the attribute length
            }
        }
    }

    private def asUtfValue(bytes: Array[Byte]): Array[Byte] = {
        serialShort(bytes.length.shortValue()) ++ bytes
    }

    private def buffSkip(n: Int): Unit = buff.position(buff.position() + n)

}

object ByteCodeRectifier {

    //Types
    val Utf8              : Byte = 1
    val Integer           : Byte = 3
    val Float             : Byte = 4
    val Long              : Byte = 5
    val Double            : Byte = 6
    val Class             : Byte = 7
    val String            : Byte = 8
    val FieldRef          : Byte = 9
    val MethodRef         : Byte = 10
    val InterfaceMethodRef: Byte = 11
    val NameAndType       : Byte = 12
    val MethodHandle      : Byte = 15
    val MethodType        : Byte = 16
    val InvokeDynamic     : Byte = 18
    val Module            : Byte = 19
    val Package           : Byte = 20

    val Flags: Array[String] = Array(
        null, "Utf8", null, "Integer", "Float",
        "Long", "Double", "Class", "String",
        "FieldRef", "MethodRef", "InterfaceMethodRef",
        "NameAndType", null, null, "MethodHandle",
        "MethodType", null, "InvokeDynamic", "Module", "Package"
    )

    //used AccessFlags that are not in the java's reflection public api
    val Access_Synthetic = 0x00001000

    //Small Optimisation tricks
    private val zero = serialShort(0)
    private val one  = serialShort(1)

    implicit private def shortConversion(int: Int): Short = int.toShort
}
