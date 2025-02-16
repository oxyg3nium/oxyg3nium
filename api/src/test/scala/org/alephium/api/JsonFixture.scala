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

package org.oxyg3nium.api

import org.scalatest.Assertion

import org.oxyg3nium.json.Json._
import org.oxyg3nium.util.{Oxyg3niumSpec, Duration}

trait JsonFixture extends ApiModelCodec with Oxyg3niumSpec {

  val blockflowFetchMaxAge = Duration.unsafe(1000)

  def checkData[T: Reader: Writer](
      data: T,
      jsonRaw: String,
      dropWhiteSpace: Boolean = true
  ): Assertion = {
    write(data) is jsonRaw.filterNot(v => dropWhiteSpace && v.isWhitespace)
    read[T](jsonRaw) is data
  }
}
