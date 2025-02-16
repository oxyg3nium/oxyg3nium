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

package org.oxyg3nium.protocol.message

import org.oxyg3nium.serde.{u32Serde, Serde}
import org.oxyg3nium.util.SecureAndSlowRandom
import org.oxyg3nium.util.U32

final case class RequestId(value: U32) {
  override def toString(): String = {
    s"RequestId: ${value.v}"
  }
}

object RequestId {
  implicit val serde: Serde[RequestId] = Serde.forProduct1(RequestId(_), _.value)

  def unsafe(value: Int): RequestId = {
    RequestId(U32.unsafe(value))
  }

  def random(): RequestId = {
    RequestId(SecureAndSlowRandom.nextNonZeroU32())
  }
}
