package fr.linkit.api.connection.cache.obj.behavior.member

trait ValueBehavior[A, C] {

    def modifyForRemoteMI(a: A, context: C): Unit

    def modifyForRequestedMI(a: A, context: C): Unit

    def modifyForLMI(a: A, context: C): Unit

    val haveAnyModifier: Boolean

}
