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

package org.oxyg3nium.util

class MathSpec extends Oxyg3niumSpec {
  it should "compare" in {
    case class OrderedClass(n: Int) extends Ordered[OrderedClass] {
      def compare(that: OrderedClass) = this.n - that.n
    }

    Math.max(OrderedClass(1), OrderedClass(2)) is OrderedClass(2)
    Math.max(OrderedClass(2), OrderedClass(1)) is OrderedClass(2)
    Math.min(OrderedClass(1), OrderedClass(2)) is OrderedClass(1)
    Math.min(OrderedClass(2), OrderedClass(1)) is OrderedClass(1)
  }
}
