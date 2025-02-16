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

package org.oxyg3nium.protocol.mining

import org.oxyg3nium.protocol.model.NoIndexModelGenerators
import org.oxyg3nium.serde._
import org.oxyg3nium.util.Oxyg3niumSpec

class PoWSpec extends Oxyg3niumSpec with NoIndexModelGenerators {
  it should "check PoW" in {
    forAll(blockGen) { block =>
      val headerBlob = serialize(block.header)
      PoW.checkMined(block, block.chainIndex) is
        PoW.checkMined(block.chainIndex, headerBlob, block.target)
    }
  }
}
