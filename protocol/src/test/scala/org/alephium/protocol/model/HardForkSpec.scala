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

import org.oxyg3nium.util.Oxyg3niumSpec

class HardForkSpec extends Oxyg3niumSpec {
  it should "compare hard fork version" in {
    (HardFork.Leman > HardFork.Mainnet) is true
    (HardFork.Rhone > HardFork.Leman) is true
    (HardFork.Rhone > HardFork.Mainnet) is true
    HardFork.Mainnet.version is 0
    HardFork.Leman.version is 1
    HardFork.Rhone.version is 2

    HardFork.Leman.isLemanEnabled() is true
    HardFork.Leman.isRhoneEnabled() is false
    HardFork.Mainnet.isLemanEnabled() is false
    HardFork.Mainnet.isRhoneEnabled() is false
    HardFork.Rhone.isRhoneEnabled() is true

    Seq(HardFork.Leman, HardFork.Rhone).contains(HardFork.SinceLemanForTest) is true
  }
}
