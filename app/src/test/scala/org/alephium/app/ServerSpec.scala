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

package org.oxyg3nium.app

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minutes, Span}

import org.oxyg3nium.flow.setting.Oxyg3niumConfigFixture
import org.oxyg3nium.util.{Oxyg3niumSpec, SocketUtil}

class ServerSpec extends Oxyg3niumSpec with ScalaFutures with SocketUtil {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(1, Minutes))

  it should "start and stop correctly" in new Oxyg3niumConfigFixture {
    override val configValues = configPortsValues

    implicit val apiConfig: ApiConfig = ApiConfig.load(newConfig)

    val flowSystem: ActorSystem = ActorSystem("flow", newConfig)
    implicit val executionContext: ExecutionContext =
      scala.concurrent.ExecutionContext.Implicits.global

    val server = Server(rootPath, flowSystem)

    server.start().futureValue is ()
    server.stop().futureValue is ()
    server.storages.dESTROYUnsafe()
  }
}
