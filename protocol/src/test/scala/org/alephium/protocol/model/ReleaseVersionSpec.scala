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

package org.oxyg3nium.protocol.model

import akka.util.ByteString

import org.oxyg3nium.protocol.Generators
import org.oxyg3nium.protocol.config.NetworkConfig
import org.oxyg3nium.util.{AlephiumSpec, TimeStamp}

class ReleaseVersionSpec extends AlephiumSpec {
  it should "get version from release string" in {
    forAll(Generators.versionGen) { case (versionStr, version) =>
      ReleaseVersion.from(versionStr) contains version
    }

    val release = "0.1.1-rc1"
    ReleaseVersion.from(release) is Some(ReleaseVersion(0, 1, 1))

    val releaseV = "v0.1.1-rc1"
    ReleaseVersion.from(releaseV) is Some(ReleaseVersion(0, 1, 1))
  }

  // scalastyle:off no.equal
  it should "have order" in {
    forAll(Generators.versionGen) { case (_, version1) =>
      forAll(Generators.versionGen) { case (_, version2) =>
        val compareResult = version1.major > version2.major ||
          version1.major == version2.major && version1.minor > version2.minor ||
          version1.major == version2.major && version1.minor == version2.minor && version1.patch > version2.patch

        (version1 > version2) is compareResult
      }
    }
  }

  it should "check client id" in {
    def buildNetworkConfig(_networkId: NetworkId, rhoneActivationTS: TimeStamp): NetworkConfig = {
      new NetworkConfig {
        override def networkId              = _networkId
        override def noPreMineProof         = ByteString.empty
        override def lemanHardForkTimestamp = TimeStamp.zero
        override def rhoneHardForkTimestamp = rhoneActivationTS
      }
    }

    val now = TimeStamp.now()

    {
      info("Test mainnet pre-Rhone")
      implicit val config = buildNetworkConfig(NetworkId.AlephiumMainNet, now.plusHoursUnsafe(1))
      config.getHardFork(now) is HardFork.Leman
      ReleaseVersion.fromClientId("xxx") is None
      ReleaseVersion.fromClientId("scala-oxyg3nium/v2.8.1/Linux") is Some(ReleaseVersion(2, 8, 1))
      ReleaseVersion.fromClientId("scala-oxyg3nium/v3.0.0/Linux") is Some(ReleaseVersion(3, 0, 0))
      ReleaseVersion.fromClientId("scala-oxyg3nium/v3.1.1/Linux") is Some(ReleaseVersion(3, 1, 1))
    }

    {
      info("Test mainnet Rhone")
      implicit val config = buildNetworkConfig(NetworkId.AlephiumMainNet, now.plusHoursUnsafe(-1))
      config.getHardFork(now) is HardFork.Rhone
      ReleaseVersion.fromClientId("xxx") is None
      ReleaseVersion.fromClientId("scala-oxyg3nium/v2.8.1/Linux") is None
      ReleaseVersion.fromClientId("scala-oxyg3nium/v3.0.0/Linux") is Some(ReleaseVersion(3, 0, 0))
      ReleaseVersion.fromClientId("scala-oxyg3nium/v3.1.1/Linux") is Some(ReleaseVersion(3, 1, 1))
    }

    {
      info("Test testnet pre-Rhone")
      implicit val config = buildNetworkConfig(NetworkId.AlephiumTestNet, now.plusHoursUnsafe(1))
      config.getHardFork(now) is HardFork.Leman
      ReleaseVersion.fromClientId("xxx") is None
      ReleaseVersion.fromClientId("scala-oxyg3nium/v2.14.5/Linux") is Some(ReleaseVersion(2, 14, 5))
      ReleaseVersion.fromClientId("scala-oxyg3nium/v2.14.6/Linux") is Some(ReleaseVersion(2, 14, 6))
      ReleaseVersion.fromClientId("scala-oxyg3nium/v3.0.0/Linux") is Some(ReleaseVersion(3, 0, 0))
    }

    {
      info("Test testnet Rhone")
      implicit val config = buildNetworkConfig(NetworkId.AlephiumTestNet, now.plusHoursUnsafe(-1))
      config.getHardFork(now) is HardFork.Rhone
      ReleaseVersion.fromClientId("xxx") is None
      ReleaseVersion.fromClientId("scala-oxyg3nium/v2.14.5/Linux") is None
      ReleaseVersion.fromClientId("scala-oxyg3nium/v2.14.6/Linux") is Some(ReleaseVersion(2, 14, 6))
      ReleaseVersion.fromClientId("scala-oxyg3nium/v3.0.0/Linux") is Some(ReleaseVersion(3, 0, 0))
    }
  }
}
