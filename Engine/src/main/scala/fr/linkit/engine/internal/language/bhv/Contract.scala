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

package fr.linkit.engine.internal.language.bhv

import fr.linkit.api.gnom.cache.sync.contract.descriptors.ContractDescriptorData
import fr.linkit.engine.internal.language.bhv.parsers.BehaviorLanguageParser

import scala.util.parsing.input.CharSequenceReader

object Contract {

    def apply(file: String): ContractDescriptorData = {
        val source = new String(getClass.getResourceAsStream(file).readAllBytes())
        val ast = BehaviorLanguageParser.parse(new CharSequenceReader(source))
        println(ast)
        null
    }


}
