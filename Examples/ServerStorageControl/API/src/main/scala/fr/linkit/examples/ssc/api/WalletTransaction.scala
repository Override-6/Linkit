package fr.linkit.examples.ssc.api

import java.sql.Timestamp

case class WalletTransaction(id: Int, amount: Int, from: UserWallet, to: UserWallet, timestamp: Timestamp)
