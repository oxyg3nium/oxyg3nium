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

import org.oxyg3nium.crypto.HashUtils
import org.oxyg3nium.protocol.Hash
import org.oxyg3nium.serde.{RandomBytes, Serde}
import org.oxyg3nium.util.Bytes.byteStringOrdering

final case class TransactionId private (value: Hash) extends AnyVal with RandomBytes {
  def length: Int = TransactionId.length

  def bytes: ByteString = value.bytes
}

object TransactionId extends HashUtils[TransactionId] {
  implicit val serde: Serde[TransactionId] = Serde.forProduct1(TransactionId.apply, t => t.value)
  implicit val transactionIdOrder: Ordering[TransactionId] = Ordering.by(_.bytes)

  lazy val zero: TransactionId = TransactionId(Hash.zero)
  val length: Int              = Hash.length

  def generate: TransactionId = TransactionId(Hash.generate)

  def from(bytes: ByteString): Option[TransactionId] = {
    Hash.from(bytes).map(TransactionId.apply)
  }

  @inline def hash(bytes: Seq[Byte]): TransactionId = {
    TransactionId(Hash.hash(bytes))
  }

  @inline def hash(str: String): TransactionId = hash(ByteString(str))

  @inline def unsafe(hash: Hash): TransactionId = TransactionId(hash)
}
