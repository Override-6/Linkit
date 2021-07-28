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

import fr.linkit.api.connection.cache.repo.PuppetWrapper
import fr.linkit.engine.local.utils.ScalaUtils.{allocate, retrieveAllFields}
import fr.linkit.engine.local.utils.{JavaUtils, ScalaUtils, UnWrapper}

import java.lang.reflect.Modifier
import scala.collection.mutable

//TODO Factorise this class and optimize it.
object CloneHelper {

    def instantiateFromOrigin[A](wrapperClass: Class[A with PuppetWrapper[A]], origin: A): A with PuppetWrapper[A] = {
        instantiateFromOrigin0(wrapperClass, deepClone(origin))
    }

    private def instantiateFromOrigin0[A](wrapperClass: Class[A with PuppetWrapper[A]], origin: A): A with PuppetWrapper[A] = {
        val instance      = allocate[A with PuppetWrapper[A]](wrapperClass)
        val checkedFields = mutable.HashSet.empty[AnyRef]

        def scanObject(instanceField: Any, originField: Any, root: Boolean): Unit = {
            if (originField == null)
                return
            val fields = retrieveAllFields(originField.getClass)
            fields.foreach(field => {
                val originValue = field.get(originField)
                if (JavaUtils.sameInstance(originValue, origin))
                    ScalaUtils.setValue(instanceField, field, instance)
                else {
                    if (root)
                        ScalaUtils.setValue(instanceField, field, originValue)
                    if (!checkedFields.contains(originValue)) {
                        checkedFields += originValue
                        originValue match {
                            case array: Array[AnyRef] => scanArray(array)
                            case _                    => scanObject(field.get(instanceField), originValue, false)
                        }
                    }
                }
            })
            fields
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

    def deepClone[A](origin: A): A = {
        val checkedItems = mutable.HashMap.empty[Any, Any]

        def getClonedInstance(data: Any): Any = {
            data match {
                case null                                 => null
                case array: Array[AnyRef]                 => java.util.Arrays.copyOf(array, array.length).map(getClonedInstance)
                case str: String                          => str
                case o if UnWrapper.isPrimitiveWrapper(o) => o
                case _                                    =>

                    val opt = checkedItems.get(data)
                    opt.getOrElse {
                        val clazz = data.getClass

                        if (!clazz.isArray) {
                            val instance = allocate[Any](clazz)
                            checkedItems.put(data, instance)
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
    }

    def detachedWrapperClone[A](origin: PuppetWrapper[A]): A = {
        val instance      = allocate[AnyRef](origin.getWrappedClass)
        val checkedFields = mutable.HashMap.empty[AnyRef, AnyRef]

        def scanObject(instanceField: Any, originField: Any, root: Boolean): Unit = {
            if (originField == null)
                return
            val fields = retrieveAllFields(instanceField.getClass)
            fields.foreach(field => {
                var originValue = field.get(originField)
                if (JavaUtils.sameInstance(originValue, origin)) {
                    ScalaUtils.setValue(instanceField, field, instance)
                }
                else {
                    originValue match {
                        case wrapper: PuppetWrapper[AnyRef] =>
                            originValue = checkedFields.getOrElseUpdate(wrapper, wrapper.detachedSnapshot())
                            ScalaUtils.setValue(instanceField, field, originValue)
                        case _                              =>
                    }
                    if (root)
                        ScalaUtils.setValue(instanceField, field, originValue)
                    if (!checkedFields.contains(originValue)) {
                        checkedFields.put(originValue, originValue)
                        originValue match {
                            case array: Array[AnyRef] => scanArray(array)
                            case _                    => scanObject(field.get(instanceField), originValue, false)
                        }
                    }
                }
            })
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
