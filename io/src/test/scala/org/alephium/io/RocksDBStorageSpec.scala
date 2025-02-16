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

package org.oxyg3nium.io

import org.oxyg3nium.macros.EnumerationMacros
import org.oxyg3nium.util.{AlephiumSpec, AVector}

class RocksDBStorageSpec extends AlephiumSpec {
  import RocksDBSource.ColumnFamily

  behavior of "RocksDBStorage"

  implicit val ordering: Ordering[ColumnFamily] = Ordering.by(_.name)

  it should "index all column family" in {
    val xs = EnumerationMacros.sealedInstancesOf[ColumnFamily]
    ColumnFamily.values is AVector.from(xs)
  }
}
