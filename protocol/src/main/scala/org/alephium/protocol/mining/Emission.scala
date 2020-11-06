// Copyright 2018 The Alephium Authors
// This file is part of the alephium project.
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

package org.alephium.protocol.mining

import java.math.BigInteger

import org.alephium.protocol.ALF
import org.alephium.protocol.config.GroupConfig
import org.alephium.protocol.model.{BlockHeader, Target}
import org.alephium.util.{Duration, TimeStamp, U256}

class Emission(groupConfig: GroupConfig) {
  val initialMaxRewardPerChain: U256         = share(Emission.initialMaxReward)
  val stableMaxRewardPerChain: U256          = share(Emission.stableMaxReward)
  val lowHashRateInitialRewardPerChain: U256 = share(Emission.lowHashRateInitialReward)

  val onePhPerSecondDivided: Target  = share(Emission.onePhPerSecond)
  val oneEhPerSecondDivided: Target  = share(Emission.oneEhPerSecond)
  val a128EhPerSecondDivided: Target = share(Emission.a128EhPerSecond)

  def share(amount: U256): U256 =
    amount.divUnsafe(U256.unsafe(groupConfig.chainNum))

  def share(target: Target): Target =
    Target.unsafe(target.value.divide(BigInteger.valueOf(groupConfig.chainNum.toLong)))

  def miningReward(header: BlockHeader): U256 =
    reward(header.target, header.timestamp, ALF.GenesisTimestamp)

  def reward(target: Target, blockTs: TimeStamp, genesisTs: TimeStamp): U256 = {
    val maxReward      = rewardMax(blockTs, genesisTs)
    val adjustedReward = rewardWrtTarget(target)
    if (maxReward > adjustedReward) adjustedReward else maxReward
  }

  def rewardMax(blockTs: TimeStamp, genesisTs: TimeStamp): U256 = {
    require(blockTs >= genesisTs)
    val elapsed = blockTs.deltaUnsafe(genesisTs)
    if (elapsed >= Emission.durationToStableMaxReward) {
      stableMaxRewardPerChain
    } else {
      val reducedCents = ALF.cent(elapsed.millis / Emission.durationToDropAboutOnceCent.millis)
      initialMaxRewardPerChain.subUnsafe(reducedCents)
    }
  }

  def rewardWrtTarget(target: Target): U256 = {
    if (target <= onePhPerSecondDivided) {
      val rewardPlus =
        (initialMaxRewardPerChain subUnsafe lowHashRateInitialRewardPerChain).toBigInt
          .multiply(target.value)
          .divide(onePhPerSecondDivided.value)
      lowHashRateInitialRewardPerChain addUnsafe U256.unsafe(rewardPlus)
    } else if (target <= oneEhPerSecondDivided) {
      val rewardReduce = (initialMaxRewardPerChain subUnsafe stableMaxRewardPerChain).toBigInt
        .multiply(target.value) // we don't subtract onePhPerSecond for simplification
        .divide(oneEhPerSecondDivided.value)
      initialMaxRewardPerChain subUnsafe U256.unsafe(rewardReduce)
    } else if (target <= a128EhPerSecondDivided) {
      val rewardReduce = stableMaxRewardPerChain.toBigInt
        .multiply(target.value) // we don't subtract oneEhPerSecond for simplification
        .divide(a128EhPerSecondDivided.value)
      stableMaxRewardPerChain subUnsafe U256.unsafe(rewardReduce)
    } else {
      U256.Zero
    }
  }
}

object Emission {
  def apply(groupConfig: GroupConfig): Emission = new Emission(groupConfig)

  //scalastyle:off magic.number
  private val initialMaxReward: U256         = ALF.alf(60)
  private val stableMaxReward: U256          = ALF.alf(12)
  private val lowHashRateInitialReward: U256 = initialMaxReward.divUnsafe(U256.unsafe(8))

  val blockTargetTime: Duration             = Duration.ofSecondsUnsafe(64)
  val blocksInAboutOneYear: Long            = 365L * 24L * 60L * 60L * 1000L / blockTargetTime.millis
  val blocksToDropAboutOneCent: Long        = blocksInAboutOneYear / 300L
  val durationToDropAboutOnceCent: Duration = blockTargetTime.timesUnsafe(blocksToDropAboutOneCent)
  val blocksToStableMaxReward: Long         = blocksToDropAboutOneCent * 1200L
  val durationToStableMaxReward: Duration   = blockTargetTime.timesUnsafe(blocksToStableMaxReward)

  val onePhPerSecond: Target  = Target.unsafe(BigInteger.ONE.shiftLeft(50))
  val oneEhPerSecond: Target  = Target.unsafe(BigInteger.ONE.shiftLeft(60))
  val a128EhPerSecond: Target = Target.unsafe(BigInteger.ONE.shiftLeft(67))
  //scalastyle:on magic.number
}
