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

import scala.util.Random

import org.oxyg3nium.protocol.config.GroupConfigFixture
import org.oxyg3nium.protocol.model.{Address, ChainIndex, HardFork}
import org.oxyg3nium.protocol.vm.LockupScript
import org.oxyg3nium.util.{Oxyg3niumSpec, AVector, NumericHelpers, U256}

class OXMSpec extends Oxyg3niumSpec {
  import OXM._

  it should "use correct unit" in {
    alph(1) is nanoAlph(1).mul(U256.Billion).get
    alph(1).toBigInt.longValue() is math.pow(10, 18).longValue()
    cent(1).mulUnsafe(U256.unsafe(100)) is alph(1)

    oneAlph is alph(1)
    oneNanoAlph is nanoAlph(1)
    oneAlph is (oneNanoAlph.mulUnsafe(U256.unsafe(1000000000)))
  }

  it should "parse `x.y OXM` format" in new Fixture {
    check("1.2OXM", alph(12) / 10)
    check("1.2 OXM", alph(12) / 10)
    check("1 OXM", alph(1))
    check("1OXM", alph(1))
    check("0.1OXM", alph(1) / 10)
    check(".1OXM", alph(1) / 10)
    check(".1     OXM", alph(1) / 10)
    check("0 OXM", U256.Zero)
    check("1234.123456 OXM", alph(1234123456) / 1000000)

    val alphMax = s"${MaxOXMValue.divUnsafe(oneAlph)}"
    alphMax is "1000000000"
    check(s"$alphMax OXM", MaxOXMValue)

    fail("1.2alph")
    fail("-1.2alph")
    fail("1.2 alph")
    fail("1 Alph")
    fail("1. OXM")
    fail(". OXM")
    fail(" OXM")
    fail("0.000000000000000000001 OXM")
  }

  it should "pretty format" in new Fixture {
    def check(alphAmount: String, str: String) = {
      val amount = OXM.alphFromString(s"$alphAmount OXM").get
      OXM.prettifyAmount(amount) is s"$str OXM"
    }

    check("0", "0")
    check("1", "1.0")
    check("100", "100.0")
    check("1000", "1,000.0")
    check("1000000", "1,000,000.0")
    check("1000000.0", "1,000,000.0")
    check("1000000.011", "1,000,000.011")
    check("0.001", "0.001")
    check("0.000000001", "0.000000001")
    check("0.000000000000000001", "0.000000000000000001")
  }

  trait Fixture extends NumericHelpers {

    def check(str: String, expected: U256) = {
      OXM.alphFromString(str) is Some(expected)
    }
    def fail(str: String) = {
      OXM.alphFromString(str) is None
    }
  }

  it should "test isSequentialTxSupported" in new GroupConfigFixture.Default {
    OXM.isSequentialTxSupported(ChainIndex.unsafe(0, 0), HardFork.Rhone) is true
    OXM.isSequentialTxSupported(ChainIndex.unsafe(0, 1), HardFork.Rhone) is false
    OXM.isSequentialTxSupported(ChainIndex.unsafe(0, 0), HardFork.PreRhoneForTest) is false
    OXM.isSequentialTxSupported(ChainIndex.unsafe(0, 1), HardFork.PreRhoneForTest) is false
  }

  it should "test isTestnetMinersWhitelisted" in {
    val validMiners = AVector.from(
      OXM.testnetWhitelistedMiners.map(lockupScript =>
        Address.from(lockupScript).asInstanceOf[Address.Asset]
      )
    )
    OXM.isTestnetMinersWhitelisted(validMiners) is true
    validMiners.foreach(miner => OXM.isTestnetMinersWhitelisted(AVector(miner)) is true)
    val index        = Random.nextInt(validMiners.length)
    val invalidMiner = Address.Asset(LockupScript.p2pkh(PublicKey.generate))
    OXM.isTestnetMinersWhitelisted(validMiners.replace(index, invalidMiner)) is false
  }
}
