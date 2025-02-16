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

package org.oxyg3nium.flow.handler

import java.nio.file.Path

import akka.actor.ActorSystem
import akka.testkit.TestProbe

import org.oxyg3nium.io.IOUtils
import org.oxyg3nium.protocol.config.BrokerConfig
import org.oxyg3nium.protocol.model.ChainIndex
import org.oxyg3nium.util.{ActorRefT, Files => AFiles}

object TestUtils {
  case class AllHandlerProbs(
      flowHandler: TestProbe,
      txHandler: TestProbe,
      dependencyHandler: TestProbe,
      viewHandler: TestProbe,
      blockHandlers: Map[ChainIndex, TestProbe],
      headerHandlers: Map[ChainIndex, TestProbe]
  )

  def createAllHandlersProbe(implicit
      brokerConfig: BrokerConfig,
      system: ActorSystem
  ): (AllHandlers, AllHandlerProbs) = {
    val flowProbe   = TestProbe()
    val flowHandler = ActorRefT[FlowHandler.Command](flowProbe.ref)
    val txProbe     = TestProbe()
    val txHandler   = ActorRefT[TxHandler.Command](txProbe.ref)
    val blockHandlers = (for {
      from <- 0 until brokerConfig.groups
      to   <- 0 until brokerConfig.groups
      chainIndex = ChainIndex.unsafe(from, to)
      if chainIndex.relateTo(brokerConfig)
    } yield {
      val probe = TestProbe()
      chainIndex -> (ActorRefT[BlockChainHandler.Command](probe.ref) -> probe)
    }).toMap
    val headerHandlers = (for {
      from <- 0 until brokerConfig.groups
      to   <- 0 until brokerConfig.groups
      chainIndex = ChainIndex.unsafe(from, to)
      if !chainIndex.relateTo(brokerConfig)
    } yield {
      val probe = TestProbe()
      chainIndex -> (ActorRefT[HeaderChainHandler.Command](probe.ref) -> probe)
    }).toMap
    val dependencyProbe   = TestProbe()
    val dependencyHandler = ActorRefT[DependencyHandler.Command](dependencyProbe.ref)
    val viewProbe         = TestProbe()
    val viewHandler       = ActorRefT[ViewHandler.Command](viewProbe.ref)
    val allHandlers = AllHandlers(
      flowHandler,
      txHandler,
      dependencyHandler,
      viewHandler,
      blockHandlers.view.mapValues(_._1).toMap,
      headerHandlers.view.mapValues(_._1).toMap
    )
    val allProbes = AllHandlerProbs(
      flowProbe,
      txProbe,
      dependencyProbe,
      viewProbe,
      blockHandlers.view.mapValues(_._2).toMap,
      headerHandlers.view.mapValues(_._2).toMap
    )
    allHandlers -> allProbes
  }

  // remove all the content under the path; the path itself would be kept
  def clear(path: Path): Unit = {
    if (path.startsWith(AFiles.tmpDir)) {
      IOUtils.clearUnsafe(path)
    } else {
      throw new RuntimeException("Only files under tmp dir could be cleared")
    }
  }
}
