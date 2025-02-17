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

import org.oxyg3nium.protocol.model
import org.oxyg3nium.protocol.model.{ContractId, ContractOutput}
import org.oxyg3nium.protocol.vm.LockupScript
import org.oxyg3nium.util.{AVector, U256}

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class AssetState(attoAlphAmount: U256, tokens: Option[AVector[Token]] = None) {
  lazy val flatTokens: AVector[Token] = tokens.getOrElse(AVector.empty)

  def toContractOutput(contractId: ContractId): ContractOutput = {
    ContractOutput(
      attoAlphAmount,
      LockupScript.p2c(contractId),
      flatTokens.map(token => (token.id, token.amount))
    )
  }
}

object AssetState {
  def from(attoAlphAmount: U256, tokens: AVector[Token]): AssetState = {
    AssetState(attoAlphAmount, Some(tokens))
  }

  def from(output: model.TxOutput): AssetState = {
    AssetState.from(output.amount, output.tokens.map(pair => Token(pair._1, pair._2)))
  }
}
