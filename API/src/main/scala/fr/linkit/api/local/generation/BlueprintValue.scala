package fr.linkit.api.local.generation

trait BlueprintValue[A] {

    def replaceValues(inserter: ValueInserter, value: A): Unit
}
