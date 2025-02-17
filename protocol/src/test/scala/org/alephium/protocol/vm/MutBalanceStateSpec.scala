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

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import org.oxyg3nium.protocol.OXM
import org.oxyg3nium.protocol.config.{GroupConfig, NetworkConfigFixture}
import org.oxyg3nium.protocol.model.{TokenId, TxGenerators}
import org.oxyg3nium.util.{Oxyg3niumSpec, U256}

class MutBalanceStateSpec extends Oxyg3niumSpec {

  it should "approveOXM" in new Fixture {
    balanceState.approveOXM(lockupScript, OXM.oneAlph) is Some(())
    balanceState is MutBalanceState(
      MutBalances(ArrayBuffer((lockupScript, balancesPerLockup.copy(attoAlphAmount = U256.Zero)))),
      MutBalances(
        ArrayBuffer((lockupScript, MutBalancesPerLockup(OXM.oneAlph, mutable.Map.empty, 0)))
      )
    )

    balanceState.approveOXM(lockupScript, OXM.oneAlph) is None
  }

  it should "approveToken" in new Fixture {
    balanceState.approveToken(lockupScript, tokenId, OXM.oneAlph) is Some(())
    balanceState is MutBalanceState(
      MutBalances(
        ArrayBuffer(
          (lockupScript, balancesPerLockup.copy(tokenAmounts = mutable.Map(tokenId -> U256.Zero)))
        )
      ),
      MutBalances(
        ArrayBuffer(
          (lockupScript, MutBalancesPerLockup(U256.Zero, mutable.Map(tokenId -> OXM.oneAlph), 0))
        )
      )
    )

    balanceState.approveToken(lockupScript, tokenId, OXM.oneAlph) is None
  }

  it should "alphRemaining" in new Fixture {
    balanceState.alphRemaining(lockupScript) is Some(OXM.oneAlph)
    balanceState.alphRemaining(lockupScriptGen.sample.get) is None
  }

  it should "tokenRemaining" in new Fixture {
    balanceState.tokenRemaining(lockupScript, tokenId) is Some(OXM.oneAlph)
    balanceState.tokenRemaining(lockupScriptGen.sample.get, tokenId) is None
    balanceState.tokenRemaining(lockupScript, TokenId.generate) is None
  }

  it should "isPaying" in new Fixture {
    balanceState.isPaying(lockupScript) is true
    balanceState.isPaying(lockupScriptGen.sample.get) is false
  }

  it should "useApproved" in new Fixture {
    balanceState.approveOXM(lockupScript, OXM.oneAlph) is Some(())

    balanceState.useApproved() is MutBalanceState.from(
      MutBalances(
        ArrayBuffer(
          (lockupScript, MutBalancesPerLockup(OXM.oneAlph, mutable.Map.empty, scopeDepth))
        )
      )
    )

    balanceState is MutBalanceState(
      MutBalances(ArrayBuffer((lockupScript, balancesPerLockup.copy(attoAlphAmount = U256.Zero)))),
      MutBalances(ArrayBuffer.empty)
    )
  }

  it should "useAll" in new Fixture {
    balanceState.useAll(lockupScript) is Some(balancesPerLockup)
    balanceState.useAll(lockupScript) is None

    balanceState is MutBalanceState.from(MutBalances.empty)
  }

  it should "useAlph" in new Fixture {
    balanceState.useAlph(lockupScript, OXM.oneAlph) is Some(())

    balanceState is MutBalanceState.from(
      MutBalances(ArrayBuffer((lockupScript, balancesPerLockup.copy(attoAlphAmount = U256.Zero))))
    )

    balanceState.useAlph(lockupScript, OXM.oneAlph) is None
    balanceState.useAlph(lockupScriptGen.sample.get, OXM.oneAlph) is None
  }

  it should "useToken" in new Fixture {
    balanceState.useToken(lockupScript, tokenId, OXM.oneAlph) is Some(())

    balanceState is MutBalanceState.from(
      MutBalances(
        ArrayBuffer(
          (lockupScript, balancesPerLockup.copy(tokenAmounts = mutable.Map(tokenId -> U256.Zero)))
        )
      )
    )

    balanceState.useToken(lockupScript, tokenId, OXM.oneAlph) is None
    balanceState.useToken(lockupScriptGen.sample.get, tokenId, OXM.oneAlph) is None
  }

  trait Fixture extends TxGenerators with NetworkConfigFixture.Default {
    implicit override val groupConfig: GroupConfig =
      new GroupConfig {
        override def groups: Int = 3
      }

    val tokenId    = TokenId.generate
    val scopeDepth = 1
    val tokens     = mutable.Map(tokenId -> OXM.oneAlph)
    val balancesPerLockup =
      MutBalancesPerLockup(OXM.oneAlph, tokens, scopeDepth)
    val lockupScript = lockupScriptGen.sample.get

    val remaining = MutBalances(ArrayBuffer((lockupScript, balancesPerLockup)))

    val balanceState = MutBalanceState.from(remaining)
  }
}
