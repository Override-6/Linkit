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
import fr.linkit.api.connection.cache.obj.description.{ObjectTreeBehavior, WrapperNodeInfo}
import fr.linkit.api.connection.cache.obj.generation.ObjectWrapperInstantiator
import fr.linkit.engine.connection.cache.obj.generation.WrapperInstantiationHelper.{MaxScanDepth, deepClone}
import fr.linkit.engine.local.utils.ScalaUtils.{allocate, retrieveAllFields}
import fr.linkit.engine.local.utils.{Identity, JavaUtils, ScalaUtils, UnWrapper}
import sun.misc.Unsafe

import java.lang.ref.Cleaner
import java.lang.reflect.{Field, Modifier}
import java.nio.Buffer
import java.nio.file.WatchKey
import scala.collection.mutable

//TODO Factorise this class and optimize it.
class WrapperInstantiationHelper(wrapperFactory: ObjectWrapperInstantiator, behaviorTree: ObjectTreeBehavior) {

    def instantiateFromOrigin[A <: AnyRef](wrapperClass: Class[A with PuppetWrapper[A]],
                                           origin: A,
                                           subWrappers: Map[AnyRef, WrapperNodeInfo]): (A with PuppetWrapper[A], Map[AnyRef, PuppetWrapper[AnyRef]]) = {
        instantiateFromOrigin0(wrapperClass, origin, subWrappers)
    }

    private def instantiateFromOrigin0[A <: AnyRef](wrapperClass: Class[A with PuppetWrapper[A]],
                                                    origin: A, subWrappers: Map[AnyRef, WrapperNodeInfo]): (A with PuppetWrapper[A], Map[AnyRef, PuppetWrapper[AnyRef]]) = {
        val trustedSubWrapper       = subWrappers.map(pair => (Identity(pair._1), pair._2))
        val instance                = allocate[A with PuppetWrapper[A]](wrapperClass)
        val checkedFields           = mutable.HashSet.empty[Identity[Any]]
        val subWrappersInstantiated = mutable.HashMap.empty[AnyRef, PuppetWrapper[AnyRef]]
        var depth: Int              = 0

        def scanObject(instanceField: AnyRef, originField: AnyRef, root: Boolean): Unit = {
            if (depth >= MaxScanDepth ||
                    originField == null ||
                    UnWrapper.isPrimitiveWrapper(instanceField))
                return
            scanAllFields(instanceField, originField, root)
        }

        def scanField(field: Field, instanceField: Any, originValue: AnyRef, setField: Boolean, mayReturn: Boolean): Unit = {
            if (JavaUtils.sameInstance(originValue, origin))
                ScalaUtils.setValue(instanceField, field, instance)
            else {
                if (setField) {
                    ScalaUtils.setValue(instanceField, field, originValue)
                    if (mayReturn)
                        return
                }
                if (checkedFields.contains(Identity(originValue)))
                    return
                checkedFields += Identity(originValue)
                originValue match {
                    case array: Array[AnyRef] => scanArray(array)
                    case _: String            =>
                    case _                    =>
                        scanObject(field.get(instanceField), originValue, false)
                }
            }
        }

        def scanAllFields(instance: Any, origin: Any, root: Boolean): Unit = {
            depth += 1
            val classUsed = if (instance.isInstanceOf[PuppetWrapper[_]]) origin.getClass else instance.getClass
            retrieveAllFields(classUsed).foreach(field => {
                try {
                    var isWrapper   = false
                    var originValue = field.get(origin)
                    if (trustedSubWrapper.contains(Identity(originValue))) {
                        originValue = asWrapper(originValue)
                        isWrapper = true
                    }
                    scanField(field, instance, originValue, root || isWrapper, isWrapper)
                } catch {
                    case _: IllegalAccessException =>
                    //simply do not scan
                }
            })
            depth -= 1
        }

        def scanArray(array: Array[AnyRef]): Unit = {
            for (i <- array.indices) {
                array(i) match {
                    case x if JavaUtils.sameInstance(x, origin) => array(i) = instance
                    case e if trustedSubWrapper.contains(Identity(e))           => array(i) = asWrapper(e)
                    case obj                                    => scanObject(obj, obj, false)
                }
            }
        }

        def asWrapper(obj: AnyRef): AnyRef = {
            subWrappersInstantiated.getOrElseUpdate(obj, {
                val (subWrapper, subSubWrappersInfo) = wrapperFactory.newWrapper(obj, behaviorTree, trustedSubWrapper(Identity(obj)), subWrappers - obj)
                subWrappersInstantiated ++= subSubWrappersInfo
                subWrapper
            })
        }

        scanObject(instance, origin, true)

        wrapperClass.getDeclaredFields
                .filterNot(f => Modifier.isStatic(f.getModifiers) || Modifier.isFinal(f.getModifiers))
                .tapEach(_.setAccessible(true))
                .foreach(_.set(instance, null))
        (instance, subWrappersInstantiated.toMap)
    }

}

object WrapperInstantiationHelper {

    val MaxScanDepth: Int = 7

    def deepClone[A](origin: A): A = {
        val checkedItems = mutable.HashMap.empty[Identity[Any], Any]
        var depth: Int   = 0

        def getClonedInstance(data: Any): Any = {
            if (depth > MaxScanDepth)
                return data
            data match {
                case null                                 => null
                case None | Nil                           => data //TODO Remove the deepClone method because it was made for instantiateFromOrigin and only duplicate fields that hosts a the wrapper
                case array: Array[AnyRef]                 => java.util.Arrays.copyOf(array, array.length).map(getClonedInstance)
                case str: String                          => str
                case o if UnWrapper.isPrimitiveWrapper(o) => o
                case hidden if hidden.getClass.isHidden   => data
                case enum if enum.getClass.isEnum         => enum

                //The method will be removed, made this awful match list in order to complete a project test
                case _: Class[_]    => data
                case _: Unsafe      => data
                case _: Cleaner     => data
                case _: WatchKey    => data
                case _: ClassLoader => data
                case _: Buffer      => data

                case _ =>

                    val opt = checkedItems.get(Identity(data))
                    opt.getOrElse {
                        val clazz = data.getClass

                        if (!clazz.isArray) {
                            val instance = allocate[Any](clazz)
                            checkedItems.put(Identity(data), instance)
                            depth += 1
                            retrieveAllFields(clazz).foreach(field => {
                                try {
                                    val copied = getClonedInstance(field.get(data))
                                    ScalaUtils.setValue(instance, field, copied)
                                } catch {
                                    case e: IllegalAccessException =>
                                    //Simply do not scan
                                }
                            })
                            depth -= 1
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

    def detachedWrapperClone[A <: AnyRef](origin: PuppetWrapper[A]): (A, Map[AnyRef, PuppetWrapper[_]]) = {
        detachedWrapperClone0(deepClone(origin))
    }

    private def detachedWrapperClone0[A <: AnyRef](originClone: PuppetWrapper[A]): (A, Map[AnyRef, PuppetWrapper[_]]) = {
        val checkedFields    = mutable.HashMap.empty[Identity[AnyRef], AnyRef]
        val detachedWrappers = mutable.HashMap.empty[PuppetWrapper[_], AnyRef]
        var depth            = 0

        def detachedWrapper(originClone: PuppetWrapper[_]): AnyRef = {
            if (detachedWrappers.contains(originClone))
                return detachedWrappers(originClone)
            val instance = allocate[A](originClone.getWrappedClass)
            detachedWrappers.put(originClone, instance)

            def scanObject(instanceField: Any, originField: Any, root: Boolean): Unit = {
                if (originField == null || depth > MaxScanDepth)
                    return
                val fields = retrieveAllFields(instanceField.getClass)
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
                if (JavaUtils.sameInstance(originValue, originClone)) {
                    ScalaUtils.setValue(instanceField, field, instance)
                }
                else {
                    if (root)
                        ScalaUtils.setValue(instanceField, field, originValue)
                    checkedFields.put(Identity(originValue), originValue)
                    originValue match {
                        case array: Array[AnyRef]                             => scanArray(array)
                        case wrapper: PuppetWrapper[AnyRef]                   => ScalaUtils.setValue(instanceField, field, detachedWrapper(wrapper))
                        case wrapper if UnWrapper.isPrimitiveWrapper(wrapper) => //just do not scan
                        case _                                                => scanObject(field.get(instanceField), originValue, false)
                    }
                }
            }

            def scanArray(array: Array[AnyRef]): Unit = {
                for (i <- array.indices) {
                    array(i) match {
                        case x if JavaUtils.sameInstance(x, originClone) => array(i) = instance
                        case wrapper: PuppetWrapper[AnyRef]              => array(i) = detachedWrapper(wrapper)
                        case obj                                    => scanObject(obj, obj, false)
                    }
                }
            }

            scanObject(instance, originClone, true)
            instance
        }

        val instance = detachedWrapper(originClone).asInstanceOf[A]

        detachedWrappers.remove(originClone)
        (instance, detachedWrappers.map(_.swap).toMap)
    }

    def scanSubWrappers(any: AnyRef): Set[PuppetWrapper[AnyRef]] = {
        val scannedWrappers  = mutable.HashSet.empty[PuppetWrapper[AnyRef]]
        val checkedInstances = mutable.HashSet.empty[Identity[AnyRef]]
        var depth            = 0

        def scanObject(obj: AnyRef): Unit = {
            if (obj == null || depth > MaxScanDepth || checkedInstances.contains(Identity(obj)) || UnWrapper.isPrimitiveWrapper(obj))
                return
            val clazz = obj.getClass
            checkedInstances += Identity(obj)
            obj match {
                case array: Array[AnyRef]                                    => array.foreach(scanObject)
                case wrapper: PuppetWrapper[AnyRef]                          => scannedWrappers += wrapper
                case _ if clazz.isArray && clazz.componentType().isPrimitive => //Simply do not scan
                case _: AnyRef                                               => scanAllFields(obj)

            }
        }

        def scanAllFields(obj: AnyRef): Unit = {
            retrieveAllFields(obj.getClass).foreach(field => {
                try {
                    depth += 1
                    scanObject(field.get(obj))
                    depth -= 1
                } catch {
                    case _: IllegalAccessException =>
                    //Simply do not scan
                }
            })
        }

        scanAllFields(any)

        scannedWrappers.toSet
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
