// Copyright 2018 The Oxyg3nium Authors
// This file is part of the alephium project.
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

import org.oxyg3nium.api.model.BuildTxCommon
import org.oxyg3nium.protocol.model.{Address, BlockHash}
import org.oxyg3nium.protocol.vm.{GasBox, GasPrice}
import org.oxyg3nium.util.TimeStamp

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class Sweep(
    toAddress: Address.Asset,
    lockTime: Option[TimeStamp] = None,
    gasAmount: Option[GasBox] = None,
    gasPrice: Option[GasPrice] = None,
    utxosLimit: Option[Int] = None,
    targetBlockHash: Option[BlockHash] = None
) extends BuildTxCommon
