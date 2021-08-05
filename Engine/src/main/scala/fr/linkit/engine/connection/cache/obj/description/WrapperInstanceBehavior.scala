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

package fr.linkit.engine.connection.cache.obj.description

import fr.linkit.api.connection.cache.obj.description._
import fr.linkit.api.local.generation.PuppetClassDescription

class WrapperInstanceBehavior[A] private(override val classDesc: PuppetClassDescription[A],
                                         override val treeView: ObjectTreeBehavior) extends WrapperBehavior[A] {

    private val factory = treeView.factory

    private val methods = {
        classDesc.listMethods()
                .map(factory.genMethodBehavior)
                .map(bhv => bhv.desc.methodId -> bhv)
                .toMap
    }

    private val fields = {
        classDesc.listFields()
                .map(factory.genFieldBehavior)
                .map(bhv => bhv.desc.fieldId -> bhv)
                .toMap
    }

    override def listMethods(): Iterable[MethodBehavior] = {
        methods.values
    }

    override def getMethodBehavior(id: Int): Option[MethodBehavior] = methods.get(id)

    override def listField(): Iterable[FieldBehavior] = {
        fields.values
    }

    override def getFieldBehavior(id: Int): Option[FieldBehavior] = fields.get(id)

}

object WrapperInstanceBehavior {

    def apply[A](classDesc: PuppetClassDescription[A], treeView: ObjectTreeBehavior): WrapperInstanceBehavior[A] = {
        val bhv = new WrapperInstanceBehavior(classDesc, treeView)
        treeView.put(classDesc.clazz, bhv)
        bhv
    }

}