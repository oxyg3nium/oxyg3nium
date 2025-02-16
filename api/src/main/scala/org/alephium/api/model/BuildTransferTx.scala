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
package org.oxyg3nium.api.model

import akka.util.ByteString

import org.oxyg3nium.protocol.model.BlockHash
import org.oxyg3nium.protocol.vm.{GasBox, GasPrice}
import org.oxyg3nium.util.AVector

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class BuildTransferTx(
    fromPublicKey: ByteString,
    fromPublicKeyType: Option[BuildTxCommon.PublicKeyType] = None,
    destinations: AVector[Destination],
    utxos: Option[AVector[OutputRef]] = None,
    gasAmount: Option[GasBox] = None,
    gasPrice: Option[GasPrice] = None,
    targetBlockHash: Option[BlockHash] = None
) extends BuildTxCommon
    with BuildTxCommon.FromPublicKey
