package fr.linkit.examples.ssc.api

import fr.linkit.api.gnom.cache.sync.contract.BasicInvocationRule.ONLY_CACHE_OWNER
import fr.linkit.api.gnom.cache.sync.contract.annotation.{FullRemote, MethodControl}

@FullRemote(ONLY_CACHE_OWNER)
trait UserAccount {

    @MethodControl(ONLY_CACHE_OWNER)
    def name: String

    @MethodControl(value = ONLY_CACHE_OWNER, synchronizeReturnValue = true)
    def getWallets: Seq[UserWallet]

    @MethodControl(value = ONLY_CACHE_OWNER, synchronizeReturnValue = true)
    def findWallet(name: String): UserWallet

}
