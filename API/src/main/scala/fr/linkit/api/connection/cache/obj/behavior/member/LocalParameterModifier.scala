package fr.linkit.api.connection.cache.obj.behavior.member

trait LocalParameterModifier[P] {

    def apply(param: P, comesFromRMIRequest: Boolean): P

}
