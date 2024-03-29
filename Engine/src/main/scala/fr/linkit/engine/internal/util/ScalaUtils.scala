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

package fr.linkit.engine.internal.util

import fr.linkit.api.gnom.packet.Packet
import fr.linkit.engine.gnom.cache.sync.generation.SyncClassRectifier.typeStringClass
import fr.linkit.engine.gnom.packet.UnexpectedPacketException
import fr.linkit.engine.internal.manipulation.creation.ObjectCreator
import fr.linkit.engine.internal.manipulation.invokation.ConstructorInvoker
import sun.misc.Unsafe

import java.io.{BufferedInputStream, File}
import java.lang.reflect.{AccessibleObject, Field, InaccessibleObjectException, Modifier}
import java.nio.{Buffer, ByteBuffer}
import scala.annotation.switch
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}
import scala.util.control.NonFatal

object ScalaUtils {
    
    def slowCopy[A: ClassTag](origin: Array[_ <: Any]): Array[A] = {
        val buff = new Array[A](origin.length)
        var i    = 0
        try {
            origin.foreach(anyRef => {
                buff(i) = anyRef.asInstanceOf[A]
                i += 1
            })
            buff
        } catch {
            case NonFatal(e) =>
                //println("Was Casting to " + classTag[A].runtimeClass)
                //println(s"Origin = ${origin.mkString("Array(", ", ", ")")}")
                //println(s"Failed when casting ref : ${origin(i)} at index $i")
                throw e
        }
    }
    
    def ensurePacketType[P <: Packet : ClassTag](packet: Packet): P = {
        val rClass = classTag[P].runtimeClass
        packet match {
            case p: P => p
            case null => throw new NullPointerException("Received null packet.")
            case p    => throw UnexpectedPacketException(s"Received unexpected packet type (${p.getClass.getName}), requested : ${rClass.getName}")
        }
    }
    
    implicit def deepToString(array: Array[Any]): String = {
        val sb = new mutable.StringBuilder("Array(")
        array.foreach {
            case subArray: Array[Any] => sb.append(deepToString(subArray))
            case any: Any             => sb.append(any).append(", ")
        }
        sb.dropRight(2) //remove last ", " string.
                .append(')')
                .toString()
    }
    
    implicit def deepToString(any: Any): String = {
        any match {
            case array: Array[Any] => deepToString(array)
            case any               => any.toString
        }
    }
    
    def getValue(instance: Any, field: Field): Any = {
        val clazz  = field.getType
        val offset = if (Modifier.isStatic(field.getModifiers)) TheUnsafe.staticFieldOffset(field) else TheUnsafe.objectFieldOffset(field)
        if (clazz.isPrimitive) {
            (clazz.getName: @switch) match {
                case "int"     => TheUnsafe.getInt(instance, offset)
                case "byte"    => TheUnsafe.getByte(instance, offset)
                case "short"   => TheUnsafe.getShort(instance, offset)
                case "long"    => TheUnsafe.getLong(instance, offset)
                case "double"  => TheUnsafe.getDouble(instance, offset)
                case "float"   => TheUnsafe.getFloat(instance, offset)
                case "boolean" => TheUnsafe.getBoolean(instance, offset)
                case "char"    => TheUnsafe.getChar(instance, offset)
            }
        } else {
            TheUnsafe.getObject(instance, offset)
        }
    }
    
    def getValueAnyRef(instance: Any, field: Field): AnyRef = {
        getValue(instance, field).asInstanceOf[AnyRef]
    }
    
    def setValue(instance: Any, field: Field, value: Any): Unit = {
        val fieldOffset = {
            if (instance == null)
                TheUnsafe.staticFieldOffset(field)
            else
                TheUnsafe.objectFieldOffset(field)
        }
        val cookie      = if (instance == null) TheUnsafe.staticFieldBase(field) else instance
        import fr.linkit.api.internal.util.Unwrapper.unwrap
        
        import java.lang
        
        if (value == null) {
            //if (!Modifier.isFinal(field.getModifiers))
            //    field.set(instance, null)
            return
        }
        
        val action: (Any, Long) => Unit = if (field.getType.isPrimitive) {
            value match {
                case i: Integer      => TheUnsafe.putInt(_, _, i)
                case b: lang.Byte    => TheUnsafe.putByte(_, _, b)
                case s: lang.Short   => TheUnsafe.putShort(_, _, s)
                case l: lang.Long    => TheUnsafe.putLong(_, _, l)
                case d: lang.Double  => TheUnsafe.putDouble(_, _, d)
                case f: lang.Float   => TheUnsafe.putFloat(_, _, f)
                case b: lang.Boolean => TheUnsafe.putBoolean(_, _, b)
                case c: Character    => TheUnsafe.putChar(_, _, c)
            }
        } else field.getType match {
            case c if c eq classOf[Integer]      => TheUnsafe.putObject(_, _, unwrap(value, _.intValue))
            case c if c eq classOf[lang.Byte]    => TheUnsafe.putObject(_, _, unwrap(value, _.byteValue))
            case c if c eq classOf[lang.Short]   => TheUnsafe.putObject(_, _, unwrap(value, _.shortValue))
            case c if c eq classOf[lang.Long]    => TheUnsafe.putObject(_, _, unwrap(value, _.longValue))
            case c if c eq classOf[lang.Double]  => TheUnsafe.putObject(_, _, unwrap(value, _.doubleValue))
            case c if c eq classOf[lang.Float]   => TheUnsafe.putObject(_, _, unwrap(value, _.floatValue))
            case c if c eq classOf[lang.Boolean] => TheUnsafe.putObject(_, _, unwrap(value, _.booleanValue))
            case c if c eq classOf[Character]    => TheUnsafe.putObject(_, _, unwrap(value, _.charValue))
            case _                               => TheUnsafe.putObject(_, _, value)
        }
        action(cookie, fieldOffset)
    }
    
    implicit def toPresentableString(bytes: Array[Byte]): String = {
        new String(bytes)
                .replace("\r", "R")
                .replace("\n", "N")
    }
    
    def toPresentableString(buff: ByteBuffer): String = {
        new String(buff.array(), 0, buff.limit())
                .replace("\r", "R")
                .replace("\n", "N")
    }
    
    def formatPath(path: String): String = path
            .replace("\\", File.separator)
            .replace("//", File.separator)
    
    private val TheUnsafe = findUnsafe()
    
    @throws[IllegalAccessException]
    def findUnsafe(): Unsafe = {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        for (field <- unsafeClass.getDeclaredFields) {
            if (field.getType eq unsafeClass) {
                field.setAccessible(true)
                return field.get(null).asInstanceOf[Unsafe]
            }
        }
        throw new IllegalStateException("No instance of Unsafe found")
    }
    
    def findInternalUnsafe(): jdk.internal.misc.Unsafe = {
        val unsafeClass = Class.forName("jdk.internal.misc.Unsafe")
        //val const = unsafeClass.getDeclaredConstructor()
        allocate(unsafeClass)
        //throw new IllegalStateException("No instance of Unsafe found")
    }
    
    def pasteAllFields[A](instance: A, data: A): Unit = {
        retrieveAllFields(data.getClass)
                .foreach(field => {
                    ScalaUtils.setValue(instance, field, ScalaUtils.getValue(data, field))
                })
    }
    
    @inline
    def allocate[A](clazz: Class[_]): A = {
        if (clazz eq null)
            throw new NullPointerException("class is null.")
        ObjectCreator.allocate(clazz).asInstanceOf[A]
    }
    
    def retrieveAllFields(clazz: Class[_], accessible: Boolean = true): Array[Field] = {
        var superClass  = clazz
        val superFields = ListBuffer.empty[Field]
        while (superClass != null) {
            superFields ++= superClass.getDeclaredFields
            superClass = superClass.getSuperclass
        }
        val r = superFields
                .filterNot(f => Modifier.isStatic(f.getModifiers))
        if (accessible)
            r.filter(setAccessible)
        r.toArray
    }
    
    def setAccessible(field: AccessibleObject): Boolean = {
        try {
            field.setAccessible(true)
            true
        } catch {
            case _: InaccessibleObjectException =>
                false
        }
    }
    
}
