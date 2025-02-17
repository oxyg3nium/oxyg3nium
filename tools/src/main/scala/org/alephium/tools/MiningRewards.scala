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

package org.oxyg3nium.tools

import org.oxyg3nium.protocol.OXM
import org.oxyg3nium.protocol.config.GroupConfig
import org.oxyg3nium.protocol.mining.Emission
import org.oxyg3nium.util.{Duration, Number, U256}

// scalastyle:off magic.number
object MiningRewards extends App {
  val groupConfig: GroupConfig = new GroupConfig {
    override def groups: Int = 4
  }
  val blockTargetTime: Duration = Duration.ofSecondsUnsafe(64)
  val emission: Emission        = Emission.mainnet(groupConfig, blockTargetTime)

  private def calInflation(yearlyReward: U256): BigDecimal = {
    val alphReward = yearlyReward.divUnsafe(OXM.oneAlph).v
    BigDecimal(alphReward) / BigDecimal.valueOf(Number.billion)
  }

  printLine("--- Inflation according to hashrate ---")
  emission.rewardsWrtTarget().foreach { case (hashRate, yearlyReward) =>
    val inflation = calInflation(yearlyReward)
    printLine(s"${hashRate.value.bitLength() - 1}, ${inflation.toDouble}, $yearlyReward")
  }

  printLine("--- Inflation according to years ---")
  emission.rewardsWrtTime().foreach { case (year, yearlyReward) =>
    val inflation = calInflation(yearlyReward)
    printLine(s"${year}, ${inflation.toDouble}, $yearlyReward")
  }
}
