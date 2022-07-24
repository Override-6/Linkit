/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.cache.sync.generation.sync

import fr.linkit.api.gnom.cache.sync.contract.description.MethodDescription
import ScalaBlueprintUtilities.{getParameters, getReturnType}
import fr.linkit.engine.internal.language.cbp.AbstractValueScope

class SyncMethodScope(name: String, blueprint: String, pos: Int) extends AbstractValueScope[MethodDescription](name, blueprint, pos) {

    bindValue("ReturnType" ~> getReturnType)
    bindValue("MethodName" ~> (_.javaMethod.getName))
    bindValue("MethodID" ~> (m => m.methodId.toString))
    bindValue("ParamsIn" ~> (getParameters(_, true, false)))
    bindValue("ParamsOut" ~> (getParameters(_, false, true)))
    bindValue("ParamsOutArray" ~> (getParameters(_, false, false)))
    bindValue("ParamsOutLambda" ~> getParamsOutLambda)
    bindValue("Override" ~> chooseOverride)

    private def getParamsOutLambda(desc: MethodDescription): String = {
        val result = (1 to desc.javaMethod.getParameterCount)
                .map(i => s"arg$i").mkString(", ")
        if (result.isEmpty) result else ", " + result
    }

    private def chooseOverride(desc: MethodDescription): String = {
        val method = desc.javaMethod
        val params = method.getParameterTypes
        method.getName match {
            case "toString" | "clone" | "hashCode" | "reference" | "finalize" if params.isEmpty => "override"
            case "equals" if params.length == 1 && (params(0) eq classOf[Object])               => "override"
            case _                                                                              => ""
        }
    }
}

