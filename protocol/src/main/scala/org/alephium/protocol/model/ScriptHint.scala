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

package org.oxyg3nium.protocol.model

import org.oxyg3nium.protocol.Hash
import org.oxyg3nium.protocol.config.GroupConfig
import org.oxyg3nium.util.{Bytes, DjbHash}

class ScriptHint(val value: Int) extends AnyVal {
  def groupIndex(implicit config: GroupConfig): GroupIndex = {
    val hash = Bytes.toPosInt(Bytes.xorByte(value))
    GroupIndex.unsafe(hash % config.groups)
  }
}

object ScriptHint {
  def fromHash(hash: Hash): ScriptHint = {
    fromHash(DjbHash.intHash(hash.bytes))
  }

  def fromHash(hash: Int): ScriptHint = {
    new ScriptHint(hash | 1)
  }
}
