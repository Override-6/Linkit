package fr.linkit.examples.ssc.api

import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.BasicInvocationRule.ONLY_CACHE_OWNER
import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.{FullRemote, MethodControl}

@FullRemote(ONLY_CACHE_OWNER)
trait CurrentUserWallet extends UserWallet {

    val owner: CurrentUserAccount

    @MethodControl(ONLY_CACHE_OWNER)
    def getAmount: Int

    @MethodControl(ONLY_CACHE_OWNER)
    def getHistory: Seq[WalletTransaction]
    
}
