package org.alephium.benchmark

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

import org.alephium.crypto.Keccak256
import org.alephium.flow.io.{HeaderDB, RocksDBStorage}
import org.alephium.flow.trie.MerklePatriciaTrie
import org.alephium.util.Files

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
class TrieBench {
  import RocksDBStorage.ColumnFamily

  private val tmpdir = Files.tmpDir
  private val dbname = "trie"
  private val dbPath = tmpdir.resolve(dbname)

  val dbStorage: RocksDBStorage = {
    val files = dbPath.toFile.listFiles
    if (files != null) {
      files.foreach(_.delete)
    }

    RocksDBStorage.openUnsafe(dbPath, RocksDBStorage.Compaction.SSD)
  }
  val db: HeaderDB = HeaderDB(dbStorage, ColumnFamily.All)

  val trie: MerklePatriciaTrie = MerklePatriciaTrie.createEmptyTrie(db)
  val genesisHash: Keccak256   = trie.rootHash

  @Benchmark
  def randomInsert(): Unit = {
    val keys = Array.tabulate(1 << 10) { _ =>
      val key  = Keccak256.random.bytes
      val data = Keccak256.random.bytes
      trie.putRaw(key, data)
      key
    }
    keys.foreach(trie.removeRaw)
    assert(trie.rootHash == genesisHash)
  }
}
