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

package fr.linkit.engine.internal.language.bhv.lexer.file

import fr.linkit.engine.internal.language.bhv.lexer.Value

object BehaviorLanguageValues {
    
    case class CodeBlock(sourceCode: String) extends Value(s"$${${sourceCode.dropRight(1)}}") with BehaviorLanguageToken
    
    case class Identifier(str: String) extends Value(str) with BehaviorLanguageToken
    
    case class Literal(str: String) extends Value(str) with BehaviorLanguageToken
    
    case class Number(number: String) extends Value(number) with BehaviorLanguageToken
    
    case class Bool(bool: String) extends Value(bool) with BehaviorLanguageToken
    
}
