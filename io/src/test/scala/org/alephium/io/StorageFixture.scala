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

package org.oxyg3nium.io

import java.nio.file.{Files => JFiles}

import org.oxyg3nium.crypto.Keccak256
import org.oxyg3nium.serde.Serde
import org.oxyg3nium.util.{Oxyg3niumFixture, Oxyg3niumSpec, Env, Files}

trait StorageFixture extends Oxyg3niumFixture {

  def newDBStorage(): RocksDBSource = {
    val rootPath = Files.testRootPath(Env.currentEnv)
    if (!JFiles.exists(rootPath)) {
      rootPath.toFile.mkdir()
    }
    val dbname  = s"test-db-${Keccak256.generate.toHexString}"
    val dbPath  = rootPath.resolve(dbname)
    val storage = RocksDBSource.openUnsafe(dbPath)
    Oxyg3niumSpec.addCleanTask(() => storage.dESTROYUnsafe())
    storage
  }

  def newDB[K: Serde, V: Serde](
      storage: RocksDBSource,
      cf: RocksDBSource.ColumnFamily
  ): KeyValueStorage[K, V] = {
    RocksDBKeyValueStorage[K, V](storage, cf)
  }
}
