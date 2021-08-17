package fr.linkit.api.connection.cache.obj.behavior.member

trait RemoteParameterModifier[P] {

    //TODO Enhance the possibility to get further information about where does the parameter object comes from
    def forRemote(param: P, target: String): P

}
