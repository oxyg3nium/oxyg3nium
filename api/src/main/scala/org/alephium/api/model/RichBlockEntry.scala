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

package org.oxyg3nium.api.model

import akka.util.ByteString

import org.oxyg3nium.protocol.Hash
import org.oxyg3nium.protocol.config.NetworkConfig
import org.oxyg3nium.protocol.model.{Block, BlockHash}
import org.oxyg3nium.util.{AVector, TimeStamp}

final case class RichBlockEntry(
    hash: BlockHash,
    timestamp: TimeStamp,
    chainFrom: Int,
    chainTo: Int,
    height: Int,
    deps: AVector[BlockHash],
    transactions: AVector[RichTransaction],
    nonce: ByteString,
    version: Byte,
    depStateHash: Hash,
    txsHash: Hash,
    target: ByteString,
    ghostUncles: AVector[GhostUncleBlockEntry]
)

object RichBlockEntry {
  def from(
      block: Block,
      height: Int,
      transactions: AVector[RichTransaction]
  )(implicit
      networkConfig: NetworkConfig
  ): Either[String, RichBlockEntry] = {
    val ghostUncleBlockDataEither = {
      if (block.isGenesis) {
        Right(AVector.empty[GhostUncleBlockEntry])
      } else {
        block.ghostUncleData match {
          case Right(ghostUncleData) => Right(ghostUncleData.map(GhostUncleBlockEntry.from))
          case _                     => Left(s"Invalid block ${block.hash.toHexString}")
        }
      }
    }
    ghostUncleBlockDataEither.map(ghostUncleBlockData =>
      RichBlockEntry(
        hash = block.header.hash,
        timestamp = block.header.timestamp,
        chainFrom = block.header.chainIndex.from.value,
        chainTo = block.header.chainIndex.to.value,
        height = height,
        deps = block.header.blockDeps.deps,
        transactions = transactions,
        nonce = block.header.nonce.value,
        version = block.header.version,
        depStateHash = block.header.depStateHash,
        txsHash = block.header.txsHash,
        target = block.header.target.bits,
        ghostUncles = ghostUncleBlockData
      )
    )
  }
}
