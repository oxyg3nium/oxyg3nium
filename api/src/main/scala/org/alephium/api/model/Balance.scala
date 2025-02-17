// Copyright 2018 The Oxyg3nium Authors
// This file is part of the oxyg3nium project.
//
// The library is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// The library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with the library. If not, see <http://www.gnu.org/licenses/>.

package org.oxyg3nium.api.model

import org.oxyg3nium.protocol.model.TokenId
import org.oxyg3nium.util.AVector
import org.oxyg3nium.util.U256

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class Balance(
    balance: Amount,
    balanceHint: Amount.Hint,
    lockedBalance: Amount,
    lockedBalanceHint: Amount.Hint,
    tokenBalances: Option[AVector[Token]] = None,
    lockedTokenBalances: Option[AVector[Token]] = None,
    utxoNum: Int
)

object Balance {
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def from(
      balance: Amount,
      lockedBalance: Amount,
      tokenBalances: Option[AVector[Token]],
      lockedTokenBalances: Option[AVector[Token]],
      utxoNum: Int
  ): Balance = Balance(
    balance,
    balance.hint,
    lockedBalance,
    lockedBalance.hint,
    tokenBalances,
    lockedTokenBalances,
    utxoNum
  )

  def from(
      balanceLockedUtxoNum: (U256, U256, AVector[(TokenId, U256)], AVector[(TokenId, U256)], Int)
  ): Balance = {
    val balance             = Amount(balanceLockedUtxoNum._1)
    val lockedBalance       = Amount(balanceLockedUtxoNum._2)
    val tokenBalances       = getTokenBalances(balanceLockedUtxoNum._3)
    val lockedTokenBalances = getTokenBalances(balanceLockedUtxoNum._4)
    val utxoNum             = balanceLockedUtxoNum._5
    Balance.from(balance, lockedBalance, tokenBalances, lockedTokenBalances, utxoNum)
  }

  private def getTokenBalances(balances: AVector[(TokenId, U256)]): Option[AVector[Token]] = {
    val tokenBalances = balances.map(tokenBalance => Token(tokenBalance._1, tokenBalance._2))
    Option.when(tokenBalances.nonEmpty)(tokenBalances)
  }
}
