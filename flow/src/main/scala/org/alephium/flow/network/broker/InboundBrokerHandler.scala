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

import akka.io.Tcp

import org.oxyg3nium.flow.network.CliqueManager
import org.oxyg3nium.flow.setting.NetworkSetting
import org.oxyg3nium.protocol.message.{Hello, Payload}
import org.oxyg3nium.protocol.model.CliqueInfo
import org.oxyg3nium.util.{ActorRefT, Duration}

trait InboundBrokerHandler extends BrokerHandler {
  val connectionType: ConnectionType = InboundConnection

  def selfCliqueInfo: CliqueInfo

  implicit def networkSetting: NetworkSetting

  def connection: ActorRefT[Tcp.Command]

  def cliqueManager: ActorRefT[CliqueManager.Command]

  override def handShakeDuration: Duration = networkSetting.retryTimeout

  override val brokerConnectionHandler: ActorRefT[ConnectionHandler.Command] = {
    val ref = context.actorOf(ConnectionHandler.clique(remoteAddress, connection, ActorRefT(self)))
    context watch ref
    ActorRefT(ref)
  }

  override def handShakeMessage: Payload = {
    Hello.unsafe(selfCliqueInfo.selfInterBrokerInfo, selfCliqueInfo.priKey, selfP2PVersion)
  }

  override def pingFrequency: Duration = networkSetting.pingFrequency
}
