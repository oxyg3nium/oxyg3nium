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

package org.oxyg3nium.flow.network.sync

import akka.actor.Props
import akka.testkit.TestActorRef

import org.oxyg3nium.flow.Oxyg3niumFlowActorSpec
import org.oxyg3nium.flow.core.BlockFlow
import org.oxyg3nium.flow.network.broker.BrokerHandler
import org.oxyg3nium.flow.setting.NetworkSetting
import org.oxyg3nium.protocol.config.BrokerConfig
import org.oxyg3nium.protocol.model.{BlockHash, ChainIndex}
import org.oxyg3nium.util.{AVector, UnsecureRandom}

class BlockFetcherSpec extends Oxyg3niumFlowActorSpec {
  class TestBlockFetcher(val blockflow: BlockFlow)(implicit
      val brokerConfig: BrokerConfig,
      val networkSetting: NetworkSetting
  ) extends BlockFetcher {

    override def receive: Receive = { case BlockFlowSynchronizer.BlockAnnouncement(hash) =>
      handleBlockAnnouncement(hash)
    }
  }

  object TestBlockFetcher {
    def props(blockflow: BlockFlow): Props = Props(new TestBlockFetcher(blockflow))
  }

  it should "fetch block" in {
    val maxCapacity  = brokerConfig.groupNumPerBroker * brokerConfig.groups * 10
    val blockFetcher = TestActorRef[TestBlockFetcher](TestBlockFetcher.props(blockFlow))
    blockFetcher.underlyingActor.maxCapacity is maxCapacity
    val blockHash = BlockHash.generate
    (0 until BlockFetcher.MaxDownloadTimes).foreach { _ =>
      blockFetcher ! BlockFlowSynchronizer.BlockAnnouncement(blockHash)
      expectMsg(BrokerHandler.DownloadBlocks(AVector(blockHash)))
      blockFetcher.underlyingActor.fetching.states.contains(blockHash) is true
    }
    blockFetcher ! BlockFlowSynchronizer.BlockAnnouncement(blockHash)
    expectNoMessage()

    val brokerGroup = UnsecureRandom.sample(brokerConfig.groupRange)
    val chainIndex  = ChainIndex.unsafe(brokerGroup, brokerGroup)
    val block       = emptyBlock(blockFlow, chainIndex)
    addAndCheck(blockFlow, block)
    blockFlow.containsUnsafe(block.hash) is true
    blockFetcher ! BlockFlowSynchronizer.BlockAnnouncement(block.hash)
    expectNoMessage()
    blockFetcher.underlyingActor.fetching.states.contains(block.hash) is false
  }
}
