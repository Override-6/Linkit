package fr.linkit.api.local.generation

trait ValueProvider {

    def getValue(name: String): BlueprintValue[_]

    def getAllValues(blueprint: String): Array[BlueprintValue[_]]

}
