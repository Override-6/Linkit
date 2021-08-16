package fr.linkit.api.connection.cache.obj.behavior.member

trait ParameterModifier[P] {

    def apply(param: P): P

}
