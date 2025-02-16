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

import org.oxyg3nium.protocol.BuildInfo
import org.oxyg3nium.protocol.config.NetworkConfig
import org.oxyg3nium.protocol.message.P2PVersion
import org.oxyg3nium.serde.{intSerde, Serde}
import org.oxyg3nium.util.TimeStamp

final case class ReleaseVersion(major: Int, minor: Int, patch: Int)
    extends Ordered[ReleaseVersion] {
  override def compare(that: ReleaseVersion): Int = {
    major.compare(that.major) match {
      case 0 =>
        minor.compare(that.minor) match {
          case 0   => patch.compare(that.patch)
          case res => res
        }
      case res => res
    }
  }

  override def toString: String = s"v$major.$minor.$patch"

  // scalastyle:off magic.number
  def checkRhoneUpgrade()(implicit networkConfig: NetworkConfig): Boolean = {
    if (networkConfig.getHardFork(TimeStamp.now()).isRhoneEnabled()) {
      if (networkConfig.networkId == NetworkId.Oxyg3niumMainNet) {
        this >= ReleaseVersion(3, 0, 0)
      } else if (networkConfig.networkId == NetworkId.Oxyg3niumTestNet) {
        this >= ReleaseVersion(2, 14, 6)
      } else {
        true
      }
    } else {
      true
    }
  }
}

object ReleaseVersion {
  val current: ReleaseVersion = from(BuildInfo.version).getOrElse(
    throw new RuntimeException(
      s"Invalid release version: ${BuildInfo.version}"
    )
  )

  def clientId(p2pVersion: P2PVersion): String = {
    s"scala-oxyg3nium/$current/${System.getProperty("os.name")}/${p2pVersion}"
  }

  def fromClientId(
      clientId: String
  )(implicit networkConfig: NetworkConfig): Option[ReleaseVersion] = {
    fromClientIdStr(clientId) match {
      case Some(version) if version.checkRhoneUpgrade() => Some(version)
      case _                                            => None
    }
  }

  private def fromClientIdStr(clientId: String): Option[ReleaseVersion] = {
    val parts = clientId.split("/")
    if (parts.length < 2) {
      None
    } else {
      from(parts(1))
    }
  }

  def from(release: String): Option[ReleaseVersion] = {
    val regex = """v?(\d+)\.(\d+)\.(\d+)(.*)?""".r
    release match {
      case regex(major, minor, patch, _) =>
        Option(ReleaseVersion(major.toInt, minor.toInt, patch.toInt))
      case _ => None
    }
  }

  implicit val serde: Serde[ReleaseVersion] =
    Serde.forProduct3(ReleaseVersion.apply, v => (v.major, v.minor, v.patch))
}
