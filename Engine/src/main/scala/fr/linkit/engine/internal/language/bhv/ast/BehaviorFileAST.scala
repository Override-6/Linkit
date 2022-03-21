package fr.linkit.engine.internal.language.bhv.ast

trait BehaviorFileAST {

    val classDescriptions: List[ClassDescription]
    val typesModifiers   : List[TypeModifier]
    val codeBlocks       : List[ScalaCodeBlock]
    val classImports     : List[ClassImport]
    val valueModifiers   : List[ValueModifier]
    val agreementBuilders: List[AgreementBuilder]
}