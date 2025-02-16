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

package org.oxyg3nium.protocol

import org.scalacheck.Gen

import org.oxyg3nium.protocol.config.GroupConfigFixture
import org.oxyg3nium.util.Oxyg3niumSpec

class GeneratorsSpec extends Oxyg3niumSpec with Generators with GroupConfigFixture {
  override val groups = 3

  it should "generate random hashes" in {
    val sampleSize = 1000
    val hashes     = Gen.listOfN(sampleSize, hashGen).sample.get
    hashes.toSet.size is sampleSize
  }
}
