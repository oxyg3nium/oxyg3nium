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

package org.oxyg3nium.api.model

import org.oxyg3nium.protocol.OXM
import org.oxyg3nium.util.U256

final case class Amount(value: U256) {
  override def toString: String = value.toString
  lazy val hint: Amount.Hint    = Amount.Hint(value)
}

object Amount {
  // x.x OXM format
  def from(string: String): Option[Amount] =
    OXM.alphFromString(string).map(Amount(_))

  val Zero: Amount = Amount(U256.Zero)

  final case class Hint(value: U256)

  def toAlphString(value: U256): String = {
    val dec = new java.math.BigDecimal(value.v).divide(new java.math.BigDecimal(OXM.oneAlph.v))
    s"${dec} OXM"
  }
}
