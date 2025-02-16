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

package org.oxyg3nium.flow.model

import org.oxyg3nium.protocol.model.TransactionId
import org.oxyg3nium.serde.{serdeTS, Serde}
import org.oxyg3nium.util.TimeStamp

final case class PersistedTxId(timestamp: TimeStamp, txId: TransactionId)

object PersistedTxId {
  implicit val serde: Serde[PersistedTxId] =
    Serde.forProduct2[TimeStamp, TransactionId, PersistedTxId](
      (ts, hash) => PersistedTxId(ts, hash),
      id => (id.timestamp, id.txId)
    )
}
