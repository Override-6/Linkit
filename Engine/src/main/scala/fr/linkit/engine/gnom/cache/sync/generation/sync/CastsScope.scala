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

import fr.linkit.engine.internal.language.cbp.AbstractValueScope

class CastsScope(name: String, blueprint: String, pos: Int) extends AbstractValueScope[Int](name, blueprint, pos) {

    bindValue("Count" ~> (_.toString))
    bindValue("Params" ~> (x => (1 to x).map("_" + _).mkString(",")))
    bindValue("ParamsWildcards" ~> (x => (1 to x).map(x => "_").mkString(",")))

}
