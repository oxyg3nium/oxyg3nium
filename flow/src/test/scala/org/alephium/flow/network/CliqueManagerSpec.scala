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

package org.oxyg3nium.flow.network

import akka.testkit.TestProbe

import org.oxyg3nium.flow.FlowFixture
import org.oxyg3nium.flow.handler.TestUtils
import org.oxyg3nium.flow.network.bootstrap.InfoFixture
import org.oxyg3nium.util.{ActorRefT, AlephiumActorSpec}

class CliqueManagerSpec extends AlephiumActorSpec {
  it should "become ready after connected to brokers" in new FlowFixture with InfoFixture {
    val (allHandlers, _) = TestUtils.createAllHandlersProbe
    val cliqueManager = system.actorOf(
      CliqueManager.props(
        blockFlow,
        allHandlers,
        ActorRefT(TestProbe().ref),
        ActorRefT(TestProbe().ref),
        discoverySetting.bootstrap.size
      )
    )
    val cliqueInfo = genIntraCliqueInfo(1).cliqueInfo
    cliqueInfo.brokerNum is 3
    cliqueManager ! CliqueManager.Start(cliqueInfo)
    cliqueManager ! CliqueManager.IsSelfCliqueReady
    expectMsg(false)
    cliqueManager ! IntraCliqueManager.Ready
    cliqueManager ! CliqueManager.IsSelfCliqueReady
    expectMsg(true)
  }
}
