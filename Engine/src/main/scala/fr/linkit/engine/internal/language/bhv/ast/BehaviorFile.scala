package fr.linkit.engine.internal.language.bhv.ast

trait BehaviorFile {

    val classDescriptions: List[ClassDescription]
    val typesModifiers   : List[TypeModifiers]
    val codeBlocks  : List[ScalaCodeBlock]
    val classImports: List[ClassImport]

}