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

package org.oxyg3nium.flow.network.sync

import org.oxyg3nium.flow.core.BlockFlow
import org.oxyg3nium.flow.network.broker.BrokerHandler
import org.oxyg3nium.flow.setting.NetworkSetting
import org.oxyg3nium.protocol.config.BrokerConfig
import org.oxyg3nium.protocol.model.BlockHash
import org.oxyg3nium.util.{AVector, BaseActor, TimeStamp}

object BlockFetcher {
  val MaxDownloadTimes: Int = 2
}

trait BlockFetcher extends BaseActor {
  import BlockFetcher.MaxDownloadTimes
  def networkSetting: NetworkSetting
  def brokerConfig: BrokerConfig
  def blockflow: BlockFlow

  val maxCapacity: Int = brokerConfig.groupNumPerBroker * brokerConfig.groups * 10
  val fetching =
    new FetchState[BlockHash](maxCapacity, networkSetting.syncExpiryPeriod, MaxDownloadTimes)

  def handleBlockAnnouncement(hash: BlockHash): Unit = {
    if (!blockflow.containsUnsafe(hash)) {
      val timestamp = TimeStamp.now()
      if (fetching.needToFetch(hash, timestamp)) {
        sender() ! BrokerHandler.DownloadBlocks(AVector(hash))
      }
    }
  }
}
