package fr.linkit.engine.internal.language.bhv.ast

case class FileName(fileName: String)

case class ClassImport(className: String, starCount: Int)