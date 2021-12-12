package fr.linkit.examples.ssc.api

import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.BasicInvocationRule.ONLY_CACHE_OWNER
import fr.linkit.api.gnom.cache.sync.contract.behavior.annotation.MethodControl
import fr.linkit.examples.ssc.api.transaction.WalletTransaction

trait CurrentUserWallet extends UserWallet {

    @MethodControl(ONLY_CACHE_OWNER)
    def getAmount: Int

    @MethodControl(ONLY_CACHE_OWNER)
    def getHistory: Seq[WalletTransaction]
    
}
