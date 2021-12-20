package fr.linkit.examples.ssc.api

import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.BasicInvocationRule.ONLY_CACHE_OWNER
import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.{FullRemote, MethodControl}

@FullRemote(ONLY_CACHE_OWNER)
trait UserAccountContainer {

    @MethodControl(value = ONLY_CACHE_OWNER, synchronizeReturnValue = true)
    def getCurrentAccount: CurrentUserAccount

    @MethodControl(value = ONLY_CACHE_OWNER, synchronizeReturnValue = true)
    def getAccount(name: String): UserAccount

    @MethodControl(ONLY_CACHE_OWNER)
    def listAccounts: Seq[UserAccount]

}
