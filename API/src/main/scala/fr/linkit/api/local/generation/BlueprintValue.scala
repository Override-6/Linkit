package fr.linkit.api.local.generation

trait BlueprintValue[A] {

    val name: String

    def replaceValues(inserter: BlueprintInserter, value: A): Unit
}
