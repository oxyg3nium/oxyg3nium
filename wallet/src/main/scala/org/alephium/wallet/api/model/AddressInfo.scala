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

package org.oxyg3nium.wallet.api.model

import org.oxyg3nium.crypto.wallet.BIP32.ExtendedPrivateKey
import org.oxyg3nium.protocol.PublicKey
import org.oxyg3nium.protocol.config.GroupConfig
import org.oxyg3nium.protocol.model.{Address, GroupIndex}

final case class AddressInfo(
    address: Address.Asset,
    publicKey: PublicKey,
    group: GroupIndex,
    path: String
)

object AddressInfo {
  def from(privateKey: ExtendedPrivateKey)(implicit config: GroupConfig): AddressInfo = {
    val publicKey = privateKey.extendedPublicKey.publicKey
    val address   = Address.p2pkh(publicKey)
    AddressInfo(address, publicKey, address.groupIndex, privateKey.derivationPath)
  }
}
