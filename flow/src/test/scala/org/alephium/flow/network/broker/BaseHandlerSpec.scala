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

package org.oxyg3nium.flow.network.broker

import akka.actor.Props

import org.oxyg3nium.flow.Oxyg3niumFlowActorSpec
import org.oxyg3nium.protocol.Generators.socketAddressGen

class BaseHandlerSpec extends Oxyg3niumFlowActorSpec {
  it should "stop the actor when deal with critical misbehaviors" in new Fixture {
    val message = MisbehaviorManager.InvalidPoW(socketAddressGen.sample.get)
    handler ! message
    expectMsg(message)
    expectTerminated(handler)
  }

  it should "stop the actor when deal with non-critical misbehaviors" in new Fixture {
    val message = MisbehaviorManager.Spamming(socketAddressGen.sample.get)
    handler ! message
    expectMsg(message)
    assertThrows[AssertionError](expectTerminated(handler))
  }

  trait Fixture {
    val props   = Props(new MockHandler)
    val handler = system.actorOf(props)

    system.eventStream.subscribe(testActor, classOf[MisbehaviorManager.Misbehavior])
    watch(handler)
  }

  class MockHandler extends BaseHandler {
    override def receive: Receive = { case misbehavior: MisbehaviorManager.Misbehavior =>
      handleMisbehavior(misbehavior)
    }
  }
}
