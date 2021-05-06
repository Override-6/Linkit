package fr.linkit.api.local.generation

trait ValueScope[A] {

    val position: Int

    val name: String

    def getSourceCode(value: A): String

}
