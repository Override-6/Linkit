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

package fr.linkit.engine.connection.cache.obj.generation.bp

import fr.linkit.api.connection.cache.obj.description.MethodDescription
import fr.linkit.engine.connection.cache.obj.generation.bp.ScalaBlueprintUtilities._
import fr.linkit.engine.local.language.cbp.AbstractValueScope

object ScalaWrapperMethodBlueprint {

    class ValueScope(name: String, blueprint: String, pos: Int) extends AbstractValueScope[MethodDescription](name, pos, blueprint) {

        bindValue("ReturnType" ~> getReturnType)
        bindValue("MethodName" ~> (_.method.getName))
        bindValue("MethodID" ~> (m => m.methodId.toString))
        bindValue("ParamsIn" ~> (getParameters(_, true)))
        bindValue("ParamsOut" ~> (getParameters(_, false)))
        bindValue("ParamsOutLambda" ~> getParamsOutLambda)
        bindValue("Override" ~> chooseOverride)

    }

    private def getParamsOutLambda(desc: MethodDescription): String = {
        val result = (1 to desc.method.getParameterCount)
                .map(i => s"arg$i").mkString(", ")
        if (result.isEmpty) result else ", " + result
    }

    private def chooseOverride(desc: MethodDescription): String = {
        val method = desc.method
        val params = method.getParameterTypes
        method.getName match {
            case "toString" | "clone" | "hashCode" if params.isEmpty              => "override"
            case "equals" if params.length == 1 && (params(0) eq classOf[Object]) => "override"
            case _                                                                => ""
        }
    }

}

