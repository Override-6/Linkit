package fr.linkit.examples.ssc.api.transaction

import java.sql.Timestamp

import fr.linkit.examples.ssc.api.UserWallet

case class WalletTransaction(id: Int, amount: Int, from: UserWallet, to: UserWallet, timestamp: Timestamp)
