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

package fr.linkit.engine.gnom.persistence.defaults.lambda

import fr.linkit.api.gnom.persistence.context.{ControlBox, TypePersistor}
import fr.linkit.api.gnom.persistence.obj.ObjectStructure
import fr.linkit.engine.gnom.persistence.EmptyObjectStructure

object NotSerializableLambdasTypePersistor$ extends TypePersistor[AnyRef] {
    
    //private       val lookup     = MethodHandles.lookup()
    //private final val LambdaMark = "$$Lambda$"
    
    override val structure: ObjectStructure = EmptyObjectStructure
    
    override def toArray(t: AnyRef): Array[Any] = {
        throw new UnsupportedOperationException("Can't serialize non serializable lambdas.")
        /*val clazz              = lambdaObject.getClass
        val fields             = clazz.getDeclaredFields
        val name               = clazz.getName
        val markIdx            = name.indexOf(LambdaMark)
        val enclosingClassName = name.take(markIdx)
        val enclosingClass     = Class.forName(enclosingClassName)

        val implMethodId = name.slice(markIdx + LambdaMark.length, name.indexOf('/') - 1)
        val fieldValues  = ObjectCreator.getAllFields(lambdaObject, fields)
        val fieldTypes   = fields.map(_.getType)
        Array(JLTPRepresentation(enclosingClass, fieldTypes, fieldValues, clazz.getInterfaces.head, implMethodId))*/
    }
    
    override def initInstance(allocatedObject: AnyRef, args: Array[Any], box: ControlBox): Unit = {
        throw new UnsupportedOperationException("Can't deserialize non serializable lambdas.")
        /*val enclosing       = representation.enclosingClass
        val interface       = representation.interface
        val interfaceMethod = interface.getDeclaredMethods.find(m => Modifier.isAbstract(m.getModifiers)).get
        val t               = MethodType.methodType(interfaceMethod.getReturnType, interfaceMethod.getParameterTypes)
        val rt              = MethodType.methodType(interface, representation.lambdaFieldTypes)
        val site            = LambdaMetafactory.metafactory(
            lookup, interface.getName,
            rt, t, lookup.findStatic(enclosing, representation.implMethodName, t), t)
        val factory         = site.getTarget
        val fieldsValues    = representation.lambdaFieldValues
        val lambda          = MethodHandleInvoker.invoke(factory, fieldsValues)
        ScalaUtils.pasteAllFields(allocatedObject, lambda)*/
    }
    
}

case class JLTPRepresentation(enclosingClass: Class[_], lambdaFieldTypes: Array[Class[_]],
                              lambdaFieldValues: Array[AnyRef], interface: Class[_], implMethodName: String)