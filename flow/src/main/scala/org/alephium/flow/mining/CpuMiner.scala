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

package org.oxyg3nium.flow.mining

import akka.actor.Props

import org.oxyg3nium.flow.client.Node
import org.oxyg3nium.flow.handler.{AllHandlers, BlockChainHandler, ViewHandler}
import org.oxyg3nium.flow.model.BlockFlowTemplate
import org.oxyg3nium.flow.setting.{Oxyg3niumConfig, MiningSetting}
import org.oxyg3nium.protocol.config.BrokerConfig
import org.oxyg3nium.protocol.model.{Block, ChainIndex}
import org.oxyg3nium.serde.serialize
import org.oxyg3nium.util.ActorRefT

object CpuMiner {
  def props(node: Node)(implicit config: Oxyg3niumConfig): Props = {
    props(node.allHandlers)(config.broker, config.mining)
  }

  def props(allHandlers: AllHandlers)(implicit
      brokerConfig: BrokerConfig,
      miningConfig: MiningSetting
  ): Props = {
    Props(new CpuMiner(allHandlers)).withDispatcher(MiningDispatcher)
  }
}

class CpuMiner(val allHandlers: AllHandlers)(implicit
    val brokerConfig: BrokerConfig,
    val miningConfig: MiningSetting
) extends Miner {

  def receive: Receive = handleMining orElse handleMiningTasks

  def subscribeForTasks(): Unit = {
    allHandlers.viewHandler ! ViewHandler.Subscribe
  }

  def unsubscribeTasks(): Unit = {
    allHandlers.viewHandler ! ViewHandler.Unsubscribe
  }

  def publishNewBlock(block: Block): Unit = {
    val blockBytes = serialize(block)
    val handlerMessage =
      BlockChainHandler.ValidateMinedBlock(block.hash, blockBytes, ActorRefT(self))
    allHandlers.getBlockHandlerUnsafe(block.chainIndex) ! handlerMessage
  }

  def handleMiningTasks: Receive = {
    case ViewHandler.NewTemplates(templates) =>
      if (miningStarted) {
        updateAndStartTasks(templates)
      }
    case BlockChainHandler.BlockAdded(hash) =>
      setIdle(ChainIndex.from(hash))
    case BlockChainHandler.InvalidBlock(hash, reason) =>
      log.error(s"Mined an invalid block ${hash.shortHex} due to: $reason")
      setIdle(ChainIndex.from(hash))
    case ViewHandler.SubscribeResult(succeeded) =>
      if (succeeded) {
        log.info(s"Subscribed for mining tasks")
      } else {
        log.warning(s"Unable to subscribe for new mining tasks")
        self ! Miner.Stop
      }
  }

  def updateAndStartTasks(templates: IndexedSeq[IndexedSeq[BlockFlowTemplate]]): Unit = {
    for {
      fromShift <- 0 until brokerConfig.groupNumPerBroker
      to        <- 0 until brokerConfig.groups
    } {
      val job = Job.from(templates(fromShift)(to))
      pendingTasks(fromShift)(to) = Some(job)
    }
    startNewTasks()
  }
}
