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

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageKeyword._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageSymbol._
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

object BehaviorFileParser extends BehaviorLanguageParser {

    private val classParser         = acceptForeign(ClassParser.parser)
    private val agreement           = acceptForeign(AgreementParser.parser)
    private val importParser        = Import ~> identifier ^^ ClassImport
    private val codeBlockParser     = Scala ~> codeBlock
    private val typeModifierParser  = Modifier ~> identifier ~ modifiers ^^ { case tpe ~ modifiers => TypeModifier(tpe, modifiers.find(_.kind == In), modifiers.find(_.kind == Out)) }
    private val valueModifierParser = {
        (identifier <~ Colon) ~ (typeParser <~ Arrow) ~ modifiers ^^ { case name ~ tpe ~ modifiers => ValueModifier(name, tpe, modifiers.find(_.kind == In), modifiers.find(_.kind == Out)) }
    }

    private val fileParser = phrase(rep(importParser | classParser | codeBlockParser | typeModifierParser | valueModifierParser | agreement))

    def parse(context: ParserContext[Elem]): BehaviorFile = {
        fileParser.apply(new TokenReader(context)) match {
            case NoSuccess(msg, n) =>
                throw new BHVLanguageException(makeErrorMessage(msg, "Failure", n.pos, context.fileSource, context.filePath))
            case Success(x, _)     =>
                unpack(x)
        }
    }

    private def unpack(roots: List[Product]): BehaviorFile = {
        val (imports, classes, blocks, tpeMods, valMods, agreements) = roots.foldLeft(
            (List[ClassImport](), List[ClassDescription](), List[ScalaCodeBlock](),
                    List[TypeModifier](), List[ValueModifier](), List[AgreementBuilder]())
        ) {
            case ((imports, b, c, d, e, f), imp: ClassImport)           => (imp :: imports, b, c, d, e, f)
            case ((a, classes, c, d, e, f), clazz: ClassDescription)    => (a, clazz :: classes, c, d, e, f)
            case ((a, b, blocks, d, e, f), block: ScalaCodeBlock)       => (a, b, block :: blocks, d, e, f)
            case ((a, b, c, tpeMods, e, f), mod: TypeModifier)          => (a, b, c, mod :: tpeMods, e, f)
            case ((a, b, c, d, valMods, f), mod: ValueModifier)         => (a, b, c, d, mod :: valMods, f)
            case ((a, b, c, d, e, agreements), value: AgreementBuilder) => (a, b, c, d, e, value :: agreements)
        }
        new BehaviorFile {
            override val classDescriptions = classes
            override val typesModifiers    = tpeMods
            override val codeBlocks        = blocks
            override val classImports      = imports
            override val valueModifiers    = valMods
            override val agreementBuilders = agreements
        }
    }

}