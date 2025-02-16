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

import org.oxyg3nium.serde.{deserialize, serialize}
import org.oxyg3nium.util.{Oxyg3niumSpec, AVector, NumericHelpers}

class BlockHeightRangeSpec extends Oxyg3niumSpec with NumericHelpers {
  it should "validate block height range" in {
    val validRanges = Seq(
      BlockHeightRange(0, 0, 1),
      BlockHeightRange(1, 1, 1),
      BlockHeightRange(1, 2, 1),
      BlockHeightRange(1, 2, 2)
    )
    validRanges.foreach(_.isValid() is true)

    val invalidRanges = Seq(
      BlockHeightRange(-1, 2, 1),
      BlockHeightRange(2, 1, 1),
      BlockHeightRange(1, 2, 0),
      BlockHeightRange(1, 2, -1)
    )
    invalidRanges.foreach(_.isValid() is false)
  }

  it should "test block height range" in {
    val from   = nextInt(0, 100)
    val to     = nextInt(100, 200)
    val step   = nextInt(1, 5)
    val range0 = BlockHeightRange.fromHeight(from)
    range0.length is 1
    range0.heights is AVector(from)
    range0.at(0) is from

    val expected = AVector.from(from.to(to, step))
    val range1   = BlockHeightRange.from(from, to, step)
    range1.length is expected.length
    range1.heights is expected
    (0 until range1.length).foreach { index =>
      range1.at(index) is expected(index)
    }

    deserialize[BlockHeightRange](serialize(range1)) isE range1
  }
}
