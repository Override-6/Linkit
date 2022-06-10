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

package fr.linkit.engine.internal.language.bhv.parser

import fr.linkit.engine.internal.language.bhv.ast.{AutoChip, ContractOption}
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageSymbol._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageValues.Identifier

object OptionParser extends BehaviorLanguageParser {
    
    
    private val autochip = makeOption("autochip", bool ^^ AutoChip)
    
    private def makeOption(name: String, parser: Parser[ContractOption]): Parser[ContractOption] = {
        At ~> Identifier(name) ~> ParenLeft ~> parser <~ ParenRight
    }
    
    private[parser] val parser: Parser[ContractOption] = autochip
    
}
