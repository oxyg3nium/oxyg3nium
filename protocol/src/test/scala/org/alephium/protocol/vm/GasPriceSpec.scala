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
import org.oxyg3nium.protocol.model.{coinbaseGasPrice, nonCoinbaseMinGasPrice, HardFork}
import org.oxyg3nium.util.{Oxyg3niumSpec, AVector, NumericHelpers}

class GasPriceSpec extends Oxyg3niumSpec with NumericHelpers {
  it should "validate gas price bounds deprecated" in {
    val (isCoinbase, hardfork) =
      AVector(
        true  -> HardFork.Mainnet,
        true  -> HardFork.SinceLemanForTest,
        false -> HardFork.Mainnet
      ).sample()
    GasPrice.validate(coinbaseGasPrice, isCoinbase, hardfork) is true
    GasPrice.validate(
      GasPrice(coinbaseGasPrice.value - 1),
      isCoinbase,
      hardfork
    ) is false
    GasPrice.validate(GasPrice(OXM.MaxOXMValue), isCoinbase, hardfork) is false
    GasPrice.validate(GasPrice(OXM.MaxOXMValue - 1), isCoinbase, hardfork) is true
  }

  it should "validate gas price bounds for non-coinbase + Leman fork" in {
    GasPrice.validate(coinbaseGasPrice, isCoinbase = false, HardFork.SinceLemanForTest) is false
    GasPrice.validate(
      nonCoinbaseMinGasPrice,
      isCoinbase = false,
      HardFork.SinceLemanForTest
    ) is true
    GasPrice.validate(
      GasPrice(nonCoinbaseMinGasPrice.value - 1),
      isCoinbase = false,
      HardFork.SinceLemanForTest
    ) is false
    GasPrice.validate(
      GasPrice(OXM.MaxOXMValue),
      isCoinbase = false,
      HardFork.SinceLemanForTest
    ) is false
    GasPrice.validate(
      GasPrice(OXM.MaxOXMValue - 1),
      isCoinbase = false,
      HardFork.SinceLemanForTest
    ) is true
  }
}
