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

package org.oxyg3nium.wallet.json

import org.oxyg3nium.api.ApiModelCodec
import org.oxyg3nium.api.UtilJson._
import org.oxyg3nium.crypto.wallet.Mnemonic
import org.oxyg3nium.json.Json.{ReadWriter => RW, _}
import org.oxyg3nium.protocol.config.GroupConfig
import org.oxyg3nium.wallet.api.model._

@SuppressWarnings(Array("org.wartremover.warts.ToString"))
trait ModelCodecs extends ApiModelCodec {

  implicit def groupConfig: GroupConfig

  implicit val addressesRW: RW[Addresses] = macroRW

  implicit val addressInfoRW: RW[AddressInfo] = macroRW

  implicit val minerAddressesInfoRW: RW[MinerAddressesInfo] = macroRW

  implicit val addressBalanceRW: RW[Balances.AddressBalance] = macroRW

  implicit val balancesRW: RW[Balances] = macroRW

  implicit val changeActiveAddressRW: RW[ChangeActiveAddress] = macroRW

  implicit val transferRW: RW[Transfer] = macroRW

  implicit val signTransactionRW: RW[Sign] = macroRW

  implicit val signTransactionResultRW: RW[SignResult] = macroRW

  implicit val sweepAllRW: RW[Sweep] = macroRW

  implicit val transferResultRW: RW[TransferResult] = macroRW

  implicit val transferResultsRW: RW[TransferResults] = macroRW

  implicit val mnemonicRW: RW[Mnemonic] = readwriter[String].bimap[Mnemonic](
    _.toLongString,
    { input =>
      Mnemonic
        .from(input)
        .getOrElse(throw upickle.core.Abort(s"Cannot validate mnemonic: $input"))
    }
  )

  implicit val walletUnlockRW: RW[WalletUnlock] = macroRW

  implicit val walletRestoreRW: RW[WalletRestore] = macroRW

  implicit val walletRestoreResultRW: RW[WalletRestoreResult] = macroRW

  implicit val walletCreationRW: RW[WalletCreation] = macroRW

  implicit val walletCreationResultRW: RW[WalletCreationResult] = macroRW

  implicit val walletSatusRW: RW[WalletStatus] = macroRW

  implicit val revealMnemonicRW: RW[RevealMnemonic] = macroRW

  implicit val revealMnemonicResultRW: RW[RevealMnemonicResult] = macroRW
}
