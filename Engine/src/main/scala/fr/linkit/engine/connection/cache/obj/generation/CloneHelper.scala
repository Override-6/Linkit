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

package fr.linkit.engine.connection.cache.obj.generation

import fr.linkit.api.connection.cache.obj.PuppetWrapper
import fr.linkit.engine.local.utils.ScalaUtils.{allocate, retrieveAllAccessibleFields}
import fr.linkit.engine.local.utils.{Identity, JavaUtils, ScalaUtils, UnWrapper}

import java.lang.reflect.{Field, Modifier}
import scala.collection.mutable

//TODO Factorise this class and optimize it.
object CloneHelper {

    val MaxScanDepth: Int = 25

    def instantiateFromOrigin[A](wrapperClass: Class[A with PuppetWrapper[A]], origin: A): A with PuppetWrapper[A] = {
        instantiateFromOrigin0(wrapperClass, origin)
    }

    private def instantiateFromOrigin0[A](wrapperClass: Class[A with PuppetWrapper[A]], origin: A): A with PuppetWrapper[A] = {
        val instance      = allocate[A with PuppetWrapper[A]](wrapperClass)
        val checkedFields = mutable.HashSet.empty[Identity[Any]]
        var depth: Int = 0

        def scanObject(instanceField: Any, originField: Any, root: Boolean): Unit = {
            if (depth >= MaxScanDepth ||
                    originField == null ||
                    checkedFields.contains(Identity(originField)) ||
                    UnWrapper.isPrimitiveWrapper(instanceField))
                return
            val fields = retrieveAllAccessibleFields(originField.getClass, originField)
            fields.foreach(field => {
                try {
                    val originValue = field.get(originField)
                    depth += 1
                    scanField(field, instanceField, originValue, root)
                    depth -= 1
                } catch {
                    case _: IllegalAccessException =>
                    //simply do not scan
                }
            })
        }

        def scanField(field: Field, instanceField: Any, originValue: AnyRef, root: Boolean): Unit = {
            if (JavaUtils.sameInstance(originValue, origin))
                ScalaUtils.setValue(instanceField, field, instance)
            else {
                if (root) {
                    ScalaUtils.setValue(instanceField, field, originValue)
                }
                checkedFields += Identity(originValue)
                originValue match {
                    case array: Array[AnyRef] => scanArray(array)
                    case _                    => scanObject(field.get(instanceField), originValue, false)
                }
            }
        }

        def scanArray(array: Array[AnyRef]): Unit = {
            for (i <- array.indices) {
                array(i) match {
                    case x if JavaUtils.sameInstance(x, origin) => array(i) = instance
                    case obj                                    => scanObject(obj, obj, false)
                }
            }
        }

        scanObject(instance, origin, true)

        wrapperClass.getDeclaredFields
                .filterNot(f => Modifier.isStatic(f.getModifiers) || Modifier.isFinal(f.getModifiers))
                .tapEach(_.setAccessible(true))
                .foreach(_.set(instance, null))
        instance
    }

    /*def deepClone[A](origin: A): A = {
        val checkedItems = mutable.HashMap.empty[Identity[Any], Any]

        def getClonedInstance(data: Any): Any = {
            data match {
                case null                                 => null
                case None | Nil                           => data //TODO Remove the deepClone method because it was made for instantiateFromOrigin and only duplicate fields that hosts a the wrapper
                case array: Array[AnyRef]                 => java.util.Arrays.copyOf(array, array.length).map(getClonedInstance)
                case str: String                          => str
                case o if UnWrapper.isPrimitiveWrapper(o) => o
                case hidden if hidden.getClass.isHidden   => data
                case enum if enum.getClass.isEnum         => enum

                    //The method will be removed, made this awful match list in order to complete a project test
                case _: Class[_]                          => data
                case _: Unsafe                            => data
                case _: Cleaner                           => data
                case _: WatchKey                          => data
                case _: ClassLoader                          => data
                case _: Buffer                            => data

                case _ =>

                    val opt = checkedItems.get(Identity(data))
                    opt.getOrElse {
                        val clazz = data.getClass

                        if (!clazz.isArray) {
                            val instance = allocate[Any](clazz)
                            checkedItems.put(Identity(data), instance)
                            retrieveAllFields(clazz).foreach(field => {
                                val copied = getClonedInstance(field.get(data))
                                ScalaUtils.setValue(instance, field, copied)
                            })
                            instance
                        } else {
                            //will be a primitive array
                            //TODO clone primitive arrays
                            data
                        }
                    }
            }
        }

        val clone = getClonedInstance(origin).asInstanceOf[A]
        clone
    }*/

    def detachedWrapperClone[A](origin: PuppetWrapper[A]): A = {
        val instance      = allocate[AnyRef](origin.getWrappedClass)
        val checkedFields = mutable.HashMap.empty[Identity[AnyRef], AnyRef]

        var depth: Int = 0
        def scanObject(instanceField: Any, originField: Any, root: Boolean): Unit = {
            if (originField == null || depth > MaxScanDepth)
                return
            val fields = retrieveAllAccessibleFields(instanceField.getClass)
            fields.foreach(field => {
                try {
                    val originValue = field.get(originField)
                    depth += 1
                    scanField(instanceField, originValue, field, root)
                    depth -= 1
                } catch {
                    case _: IllegalAccessException =>
                    //simply do not scan

                }
            })
        }

        def scanField(instanceField: Any, originValue: AnyRef, field: Field, root: Boolean): Unit = {
            if (JavaUtils.sameInstance(originValue, origin)) {
                ScalaUtils.setValue(instanceField, field, instance)
            }
            else {
                if (root)
                    ScalaUtils.setValue(instanceField, field, originValue)
                if (!checkedFields.contains(Identity(originValue))) {
                    checkedFields.put(Identity(originValue), originValue)
                    originValue match {
                        case array: Array[AnyRef]                             => scanArray(array)
                        case wrapper if UnWrapper.isPrimitiveWrapper(wrapper) => //just do not scan
                        case _                                                => scanObject(field.get(instanceField), originValue, false)
                    }
                }
            }
        }

        def scanArray(array: Array[AnyRef]): Unit = {
            for (i <- array.indices) {
                array(i) match {
                    case x if JavaUtils.sameInstance(x, origin) => array(i) = instance
                    case obj                                    => scanObject(obj, obj, false)
                }
            }
        }

        scanObject(instance, origin, true)

        instance.asInstanceOf[A]
    }

    def prepareClass(clazz: Class[_]): Unit = {
        clazz.getFields
        clazz.getDeclaredFields
        clazz.getMethods
        clazz.getDeclaredMethods
        clazz.getSimpleName
        clazz.getName
        clazz.getAnnotations
        clazz.getDeclaredAnnotations
        clazz.getConstructors
        clazz.getDeclaredConstructors
    }

}
