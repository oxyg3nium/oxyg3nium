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

import scala.collection.mutable

import org.oxyg3nium.flow.core.BlockFlow
import org.oxyg3nium.protocol.model.BlockHash
import org.oxyg3nium.util.{AVector, Duration, TimeStamp}

trait DownloadTracker {
  def blockflow: BlockFlow

  val syncing: mutable.HashMap[BlockHash, TimeStamp] = mutable.HashMap.empty

  private def needToDownload(hash: BlockHash): Boolean = {
    !(syncing.contains(hash) || blockflow.contains(hash).exists(identity))
  }

  def getDownloadBlockHashes(hashes: AVector[AVector[BlockHash]]): AVector[BlockHash] = {
    val currentTs  = TimeStamp.now()
    val toDownload = hashes.flatMap(_.filter(needToDownload))
    toDownload.foreach(hash => syncing.addOne(hash -> currentTs))
    toDownload
  }

  def finalized(hash: BlockHash): Unit = {
    syncing -= hash
  }

  def cleanupSyncing(aliveDuration: Duration): Int = {
    val threshold = TimeStamp.now().minusUnsafe(aliveDuration)
    val oldSize   = syncing.size
    syncing.filterInPlace { case (_, timestamp) =>
      timestamp > threshold
    }
    oldSize - syncing.size
  }
}
