package fr.linkit.api.connection.cache.obj.behavior

/**
 * Defines the rules of a remote invocation request.
 * */
trait RemoteInvocationRule {

    /**
     * The agreement builder is used to blacklist or whitelist some engines from the request.
     * Note: if the current engine is blacklisted, the local call to the object's method should not be called.
     *
     * @param agreement the builder for the agreement
     * */
    def apply(agreement: RMIRulesAgreementBuilder): Unit

}
