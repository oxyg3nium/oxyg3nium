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

package org.oxyg3nium.flow.network.interclique

import scala.util.Random

import akka.io.Tcp
import akka.testkit.{TestActorRef, TestProbe}

import org.oxyg3nium.flow.{AlephiumFlowActorSpec, FlowFixture}
import org.oxyg3nium.flow.handler.TestUtils
import org.oxyg3nium.flow.network.broker.BrokerHandler
import org.oxyg3nium.protocol.{Generators, SignatureSchema}
import org.oxyg3nium.protocol.message.{Hello, P2PV1, P2PV2}
import org.oxyg3nium.protocol.model.InterBrokerInfo
import org.oxyg3nium.util.ActorRefT

class OutboundBrokerHandlerSpec extends AlephiumFlowActorSpec {
  it should "connect to remote broker with valid broker info" in new Fixture {
    brokerHandler ! Tcp.Connected(
      expectedRemoteBroker.address,
      Generators.socketAddressGen.sample.get
    )
    val hello = Hello.unsafe(expectedRemoteBroker.interBrokerInfo, priKey, selfP2PVersion)
    brokerHandler ! BrokerHandler.Received(hello)
    brokerHandlerActor.pingPongTickOpt is a[Some[_]]
  }

  it should "not connect to remote broker with invalid broker info" in new Fixture {
    val handlerProbe = TestProbe()
    handlerProbe.watch(brokerHandler)
    brokerHandler ! Tcp.Connected(
      expectedRemoteBroker.address,
      Generators.socketAddressGen.sample.get
    )
    val wrongBroker = InterBrokerInfo.unsafe(
      Generators.cliqueIdGen.sample.get,
      expectedRemoteBroker.brokerId,
      expectedRemoteBroker.brokerNum
    )
    val hello = Hello.unsafe(wrongBroker, priKey, selfP2PVersion)
    brokerHandler ! BrokerHandler.Received(hello)
    handlerProbe.expectTerminated(brokerHandler.ref)
  }

  trait Fixture extends FlowFixture {
    val cliqueManager         = TestProbe()
    val connectionHandler     = TestProbe()
    val blockFlowSynchronizer = TestProbe()
    val maxForkDepth          = 5
    val expectedRemoteBroker  = Generators.brokerInfoGen.sample.get
    val selfP2PVersion        = if (Random.nextBoolean()) P2PV1 else P2PV2

    lazy val (priKey, pubKey)               = SignatureSchema.secureGeneratePriPub()
    lazy val (allHandler, allHandlerProbes) = TestUtils.createAllHandlersProbe
    lazy val brokerHandler = TestActorRef[OutboundBrokerHandler](
      OutboundBrokerHandler.props(
        Generators.cliqueInfoGen.sample.get,
        expectedRemoteBroker,
        blockFlow,
        allHandler,
        ActorRefT(cliqueManager.ref),
        ActorRefT(blockFlowSynchronizer.ref)
      )
    )
    lazy val brokerHandlerActor = brokerHandler.underlyingActor
  }
}
