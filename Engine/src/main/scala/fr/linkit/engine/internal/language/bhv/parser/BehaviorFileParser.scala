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

package fr.linkit.engine.internal.language.bhv.parser

import fr.linkit.engine.internal.language.bhv.BHVLanguageException
import fr.linkit.engine.internal.language.bhv.ast._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageKeyword._
import fr.linkit.engine.internal.language.bhv.lexer.file.BehaviorLanguageSymbol._
import fr.linkit.engine.internal.language.bhv.parser.ParserErrorMessageHelper.makeErrorMessage

import java.io.File

object BehaviorFileParser extends BehaviorLanguageParser {
    
    private val nameParser          = Name ~> literal ^^ FileName
    private val classParser         = acceptForeign(ClassParser.parser)
    private val agreementParser     = acceptForeign(AgreementBuilderParser.declaration)
    private val optionParser        = acceptForeign(OptionParser.parser)
    private val codeBlockParser     = Code ~> codeBlock
    //MAINTAINED private val typeModifierParser  = Modifier ~> identifier ~ modifiers ^^ { case tpe ~ modifiers => TypeModifier(tpe, modifiers.find(_.kind == ast.In), modifiers.find(_.kind == ast.Out)) }
    private val importParser        = {
        Import ~> (rep(identifier <~ Dot.?) ^^ (_.mkString("."))) ~ repNM(0, 2, Star) ^^ {
            case id ~ stars => ClassImport(id, stars.length)
        }
    }
    /*MAINTAINED private val valueModifierParser = {
        (identifier <~ Colon) ~ (typeParser <~ Arrow) ~ modifiers ^^ { case name ~ tpe ~ modifiers => ValueModifier(name, tpe, modifiers.find(_.kind == ast.In), modifiers.find(_.kind == ast.Out)) }
    }*/
    
    private val fileParser = {
        phrase(rep(nameParser | importParser | classParser |
                           codeBlockParser | agreementParser |
                           optionParser))
    }
    
    def parse(context: ParserContext[Elem]): BehaviorFileAST = try {
        val r = try {
            fileParser.apply(new TokenReader(context))
        } catch {
            case e: BHVLanguageException =>
                throw new BHVLanguageException(s"in ${context.filePath}: \n" + e.getMessage)
        }
        r match {
            case NoSuccess(msg, n) =>
                throw new BHVLanguageException(makeErrorMessage(msg, "Failure", n.pos, context.fileSource, context.filePath))
            case Success(x, _)     =>
                val fileName = context.filePath.takeWhile(_ == File.separatorChar)
                unpack(fileName, x)
        }
    }
    
    private def unpack(defaultFileName: String, roots: List[Product]): BehaviorFileAST = {
        val fileName0 = roots.headOption.map {
            case FileName(name) => name
            case _              => defaultFileName
        }.getOrElse(defaultFileName)
        
        val (optns, imports, classes, blocks, tpeMods, valMods, agreements) = roots.tail.foldLeft(
            (List[ContractOption](), List[ClassImport](), List[ClassDescription](), List[ScalaCodeBlock](),
                    List[TypeModifier](), List[ValueModifier](), List[AgreementBuilder]())
            ) {
            case ((options, b, c, d, e, f, g), opt: ContractOption)        => (opt :: options, b, c, d, e, f, g)
            case ((a, imports, c, d, e, f, g), imp: ClassImport)           => (a, imp :: imports, c, d, e, f, g)
            case ((a, b, classes, d, e, f, g), clazz: ClassDescription)    => (a, b, clazz :: classes, d, e, f, g)
            case ((a, b, c, blocks, e, f, g), block: ScalaCodeBlock)       => (a, b, c, block :: blocks, e, f, g)
            case ((a, b, c, d, tpeMods, f, g), mod: TypeModifier)          => (a, b, c, d, mod :: tpeMods, f, g)
            case ((a, b, c, d, e, valMods, g), mod: ValueModifier)         => (a, b, c, d, e, mod :: valMods, g)
            case ((a, b, c, d, e, f, agreements), value: AgreementBuilder) => (a, b, c, d, e, f, value :: agreements)
            case (_, name: FileName)                                       =>
                throw new BHVLanguageException(s"Unexpected 'name '\"$name\"' statement: must be present at the beginning of the file")
        }
        new BehaviorFileAST {
            override val fileName: String  = fileName0
            override val options           = optns
            override val classDescriptions = classes
            override val typesModifiers    = tpeMods
            override val codeBlocks        = blocks
            override val classImports      = imports
            override val valueModifiers    = valMods
            override val agreementBuilders = agreements
        }
    }
}