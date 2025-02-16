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

package org.oxyg3nium.protocol.vm

import org.oxyg3nium.protocol.OXM
import org.oxyg3nium.protocol.model.{dustUtxoAmount, Address, TokenId}
import org.oxyg3nium.protocol.vm.NotEnoughApprovedBalance
import org.oxyg3nium.util.{Oxyg3niumSpec, Hex, U256}

class ExeFailureSpec extends Oxyg3niumSpec {
  it should "test NotEnoughApprovedBalance" in {
    val lockupScript =
      Address.fromBase58("1G5fKUFGRFEHXcTeCfgKQNppbumd8E9G8vA6p2wf4n2L6").get.lockupScript
    val tokenId = TokenId
      .from(Hex.unsafe("cee16cdfad98e8943fd9a5b45e1765e0a6895608efe3eeeb41a60db55f605b46"))
      .get

    NotEnoughApprovedBalance(lockupScript, tokenId, OXM.alph(1), U256.Zero).toString is
      "Not enough approved balance for address 1G5fKUFGRFEHXcTeCfgKQNppbumd8E9G8vA6p2wf4n2L6, tokenId: cee16cdfad98e8943fd9a5b45e1765e0a6895608efe3eeeb41a60db55f605b46, expected: 1000000000000000000, got: 0"
    NotEnoughApprovedBalance(lockupScript, TokenId.alph, OXM.alph(2), OXM.oneAlph).toString is
      "Not enough approved balance for address 1G5fKUFGRFEHXcTeCfgKQNppbumd8E9G8vA6p2wf4n2L6, tokenId: OXM, expected: 2000000000000000000, got: 1000000000000000000"
  }

  it should "test InvalidOutputBalances" in {
    val address      = "1G5fKUFGRFEHXcTeCfgKQNppbumd8E9G8vA6p2wf4n2L6"
    val lockupScript = Address.fromBase58(address).get.lockupScript

    InvalidOutputBalances(lockupScript, 2, U256.Zero).toString is
      s"Invalid OXM balance for address $address, expected 0.002 OXM, got 0 OXM, you need to transfer more OXM to this address"

    val attoAlphAmount0 = dustUtxoAmount.subUnsafe(U256.One)
    InvalidOutputBalances(lockupScript, 0, attoAlphAmount0).toString is
      s"Invalid OXM balance for address $address, expected 0.001 OXM, got 0.000999999999999999 OXM, you need to transfer more OXM to this address"

    InvalidOutputBalances(lockupScript, 2, dustUtxoAmount).toString is
      s"Invalid OXM balance for address $address, expected 0.002 OXM, got 0.001 OXM, you need to transfer more OXM to this address"

    val attoAlphAmount1 = dustUtxoAmount.addUnsafe(U256.One)
    InvalidOutputBalances(lockupScript, 2, attoAlphAmount1).toString is
      s"Invalid OXM balance for address $address, expected 0.002 OXM, got 0.001000000000000001 OXM, you need to transfer more OXM to this address"

    val attoAlphAmount2 = dustUtxoAmount.mulUnsafe(U256.unsafe(3)).subUnsafe(U256.One)
    InvalidOutputBalances(lockupScript, 2, attoAlphAmount2).toString is
      s"Invalid OXM balance for address $address, expected 0.003 OXM, got 0.002999999999999999 OXM, you need to transfer more OXM to this address"
  }

  it should "test EmptyContractAsset" in {
    val address         = "22sTaM5xer7h81LzaGA2JiajRwHwECpAv9bBuFUH5rrnr"
    val contractAddress = Address.fromBase58(address).get.asInstanceOf[Address.Contract]

    EmptyContractAsset(contractAddress).toString is
      s"No assets for contract 22sTaM5xer7h81LzaGA2JiajRwHwECpAv9bBuFUH5rrnr, a minimum of 0.1 OXM is required"
  }
}
