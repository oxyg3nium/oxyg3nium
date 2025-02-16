// Copyright 2018 The Alephium Authors
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

package org.oxyg3nium.benchmark

import java.util.concurrent.TimeUnit

import scala.util.Random

import org.openjdk.jmh.annotations._

import org.oxyg3nium.flow.setting.{AlephiumConfig, Platform}
import org.oxyg3nium.protocol.config.{ConsensusConfig, GroupConfig}
import org.oxyg3nium.protocol.mining.PoW
import org.oxyg3nium.protocol.model.{Block, ChainIndex}
import org.oxyg3nium.util.AVector

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
class MiningBench {

  val config: AlephiumConfig            = AlephiumConfig.load(Platform.getRootPath(), "oxyg3nium")
  implicit val groupConfig: GroupConfig = config.broker
  implicit val consensusConfig: ConsensusConfig = config.consensus.mainnet

  @Benchmark
  def mineGenesis(): Boolean = {
    val block = Block.genesis(ChainIndex.unsafe(0, 0), AVector.empty)
    val i     = Random.nextInt(groupConfig.groups)
    val j     = Random.nextInt(groupConfig.groups)
    PoW.checkMined(block, ChainIndex.unsafe(i, j))
  }
}
