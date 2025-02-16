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
package org.oxyg3nium.protocol.vm.nodeindexes

import org.oxyg3nium.protocol.model.BlockHash
import org.oxyg3nium.protocol.vm.{BlockEnv, TxEnv}
import org.oxyg3nium.serde.{intSerde, Serde}

final case class TxOutputLocator(
    blockHash: BlockHash,
    txIndex: Int,
    txOutputIndex: Int
)

object TxOutputLocator {
  implicit val txOutputLocatorSerde: Serde[TxOutputLocator] =
    Serde.forProduct3(apply, b => (b.blockHash, b.txIndex, b.txOutputIndex))

  def from(
      blockEnv: BlockEnv,
      txEnv: TxEnv,
      txOutputIndex: Int
  ): Option[TxOutputLocator] = {
    for {
      blockHash <- blockEnv.blockId
    } yield TxOutputLocator(blockHash, txEnv.txIndex, txOutputIndex)
  }
}
