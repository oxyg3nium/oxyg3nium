// Copyright 2018 The Alephium Authors
// This file is part of the alephium project.
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

package org.alephium.flow.core

import scala.language.implicitConversions

import akka.util.ByteString
import org.scalatest.Assertion

import org.alephium.crypto.{ED25519, ED25519Signature, SecP256K1, SecP256K1Signature}
import org.alephium.flow.FlowFixture
import org.alephium.protocol.{ALF, Hash}
import org.alephium.protocol.model._
import org.alephium.protocol.vm._
import org.alephium.protocol.vm.lang.Compiler
import org.alephium.serde.serialize
import org.alephium.util.{AlephiumSpec, AVector, Hex, U256}

// scalastyle:off file.size.limit
class VMSpec extends AlephiumSpec {
  implicit def gasBox(n: Int): GasBox = GasBox.unsafe(n)

  def contractCreation(
      code: StatefulContract,
      initialState: AVector[Val],
      lockupScript: LockupScript.Asset,
      alfAmount: U256
  ): StatefulScript = {
    val address  = Address.Asset(lockupScript)
    val codeRaw  = Hex.toHexString(serialize(code))
    val stateRaw = Hex.toHexString(serialize(initialState))
    val scriptRaw =
      s"""
         |TxScript Foo {
         |  pub payable fn main() -> () {
         |    approveAlf!(@${address.toBase58}, ${alfAmount.v})
         |    createContract!(#$codeRaw, #$stateRaw)
         |  }
         |}
         |""".stripMargin
    Compiler.compileTxScript(scriptRaw).rightValue
  }

  it should "not start with private function" in new FlowFixture {
    val input =
      s"""
         |TxScript Foo {
         |  pub fn add() -> () {
         |    return
         |  }
         |}
         |""".stripMargin
    val script      = Compiler.compileTxScript(input).toOption.get
    val errorScript = StatefulScript.unsafe(AVector(script.methods.head.copy(isPublic = false)))

    val chainIndex = ChainIndex.unsafe(0, 0)
    val block      = simpleScript(blockFlow, chainIndex, errorScript)
    intercept[AssertionError](addAndCheck(blockFlow, block)).getMessage is
      s"Right(ExistInvalidTx(TxScriptExeFailed($ExternalPrivateMethodCall)))"
  }

  it should "overflow frame stack" in new FlowFixture {
    val input =
      s"""
         |TxScript Foo {
         |  pub fn main() -> () {
         |    foo(${frameStackMaxSize - 1})
         |  }
         |
         |  fn foo(n: U256) -> () {
         |    if (n > 0) {
         |      foo(n - 1)
         |    }
         |  }
         |}
         |""".stripMargin
    val script = Compiler.compileTxScript(input).toOption.get

    val chainIndex = ChainIndex.unsafe(0, 0)
    val block      = simpleScript(blockFlow, chainIndex, script)
    val tx = {
      val txTemplate = block.transactions.head
      txTemplate.copy(unsigned = txTemplate.unsigned.copy(startGas = 1000000))
    }
    val worldState = blockFlow.getBestCachedWorldState(chainIndex.from).rightValue
    val blockEnv   = blockFlow.getDryrunBlockEnv(chainIndex).rightValue
    StatefulVM.runTxScript(
      worldState,
      blockEnv,
      tx,
      None,
      tx.unsigned.scriptOpt.get,
      tx.unsigned.startGas
    ) is
      failed(StackOverflow)
  }

  trait CallFixture extends FlowFixture {
    def access: String

    lazy val input0 =
      s"""
         |TxContract Foo(mut x: U256) {
         |  $access fn add(a: U256) -> () {
         |    x = x + a
         |    if (a > 0) {
         |      add(a - 1)
         |    }
         |    return
         |  }
         |}
         |""".stripMargin
    lazy val script0      = Compiler.compileContract(input0).toOption.get
    lazy val initialState = AVector[Val](Val.U256(U256.Zero))

    lazy val chainIndex = ChainIndex.unsafe(0, 0)
    lazy val fromLockup = getGenesisLockupScript(chainIndex)
    lazy val txScript0  = contractCreation(script0, initialState, fromLockup, ALF.alf(1))
    lazy val block0     = payableCall(blockFlow, chainIndex, txScript0)
    lazy val contractOutputRef0 =
      TxOutputRef.unsafe(block0.transactions.head, 0).asInstanceOf[ContractOutputRef]
    lazy val contractKey0 = contractOutputRef0.key

    lazy val input1 =
      s"""
         |TxContract Foo(mut x: U256) {
         |  pub fn add(a: U256) -> () {
         |    x = x + a
         |    if (a > 0) {
         |      add(a - 1)
         |    }
         |    return
         |  }
         |}
         |
         |TxScript Bar {
         |  pub fn call() -> () {
         |    let foo = Foo(#${contractKey0.toHexString})
         |    foo.add(4)
         |    return
         |  }
         |}
         |""".stripMargin
  }

  it should "not call external private function" in new CallFixture {
    val access: String = ""

    addAndCheck(blockFlow, block0, 1)
    checkState(blockFlow, chainIndex, contractKey0, initialState, contractOutputRef0)

    val script1 = Compiler.compileTxScript(input1, 1).toOption.get
    val block1  = simpleScript(blockFlow, chainIndex, script1)
    intercept[AssertionError](addAndCheck(blockFlow, block1, 2)).getMessage is
      s"Right(ExistInvalidTx(TxScriptExeFailed($ExternalPrivateMethodCall)))"
  }

  it should "handle contract states" in new CallFixture {
    val access: String = "pub"

    addAndCheck(blockFlow, block0, 1)
    checkState(blockFlow, chainIndex, contractKey0, initialState, contractOutputRef0)

    val script1   = Compiler.compileTxScript(input1, 1).toOption.get
    val newState1 = AVector[Val](Val.U256(U256.unsafe(10)))
    val block1    = simpleScript(blockFlow, chainIndex, script1)
    addAndCheck(blockFlow, block1, 2)
    checkState(blockFlow, chainIndex, contractKey0, newState1, contractOutputRef0, numAssets = 4)

    val newState2 = AVector[Val](Val.U256(U256.unsafe(20)))
    val block2    = simpleScript(blockFlow, chainIndex, script1)
    addAndCheck(blockFlow, block2, 3)
    checkState(blockFlow, chainIndex, contractKey0, newState2, contractOutputRef0, numAssets = 6)
  }

  trait ContractFixture extends FlowFixture {
    val chainIndex     = ChainIndex.unsafe(0, 0)
    val genesisLockup  = getGenesisLockupScript(chainIndex)
    val genesisAddress = Address.Asset(genesisLockup)

    def createContract(input: String, initialState: AVector[Val]): ContractOutputRef = {
      val contract = Compiler.compileContract(input).rightValue
      val txScript = contractCreation(contract, initialState, genesisLockup, dustUtxoAmount)
      val block    = payableCall(blockFlow, chainIndex, txScript)

      val contractOutputRef =
        TxOutputRef.unsafe(block.transactions.head, 0).asInstanceOf[ContractOutputRef]

      addAndCheck(blockFlow, block)
      contractOutputRef
    }

    def createContract(
        input: String,
        numAssets: Int,
        numContracts: Int,
        initialState: AVector[Val] = AVector[Val](Val.U256(U256.Zero))
    ): ContractOutputRef = {
      val contractOutputRef = createContract(input, initialState)

      val contractKey = contractOutputRef.key
      checkState(
        blockFlow,
        chainIndex,
        contractKey,
        initialState,
        contractOutputRef,
        numAssets,
        numContracts
      )

      contractOutputRef
    }

    def callTxScript(input: String): Unit = {
      val script = Compiler.compileTxScript(input).toOption.get
      val block =
        if (script.entryMethod.isPayable) {
          payableCall(blockFlow, chainIndex, script)
        } else {
          simpleScript(blockFlow, chainIndex, script)
        }
      addAndCheck(blockFlow, block)
    }

    def callTxScriptMulti(input: Int => String): Block = {
      val block0 = transfer(blockFlow, chainIndex, numReceivers = 10)
      addAndCheck(blockFlow, block0)
      val newAddresses = block0.nonCoinbase.head.unsigned.fixedOutputs.init.map(_.lockupScript)
      val scripts = AVector.tabulate(newAddresses.length) { index =>
        Compiler.compileTxScript(input(index)).fold(throw _, identity)
      }
      val block1 = simpleScriptMulti(blockFlow, chainIndex, newAddresses, scripts)
      addAndCheck(blockFlow, block1)
      block1
    }

    def testSimpleScript(main: String) = {
      val script = Compiler.compileTxScript(main).rightValue
      val block  = simpleScript(blockFlow, chainIndex, script)
      addAndCheck(blockFlow, block)
    }

    def fail(blockFlow: BlockFlow, block: Block, failure: ExeFailure): Assertion = {
      intercept[AssertionError](addAndCheck(blockFlow, block)).getMessage is
        s"Right(ExistInvalidTx(TxScriptExeFailed($failure)))"
    }

    def fail(
        blockFlow: BlockFlow,
        chainIndex: ChainIndex,
        script: StatefulScript,
        failure: ExeFailure
    ) = {
      intercept[AssertionError](payableCall(blockFlow, chainIndex, script)).getMessage is
        s"Right(TxScriptExeFailed($failure))"
    }
  }

  it should "not use up contract assets" in new ContractFixture {
    val input =
      """
         |TxContract Foo() {
         |  pub payable fn foo(address: Address) -> () {
         |    transferAlfFromSelf!(address, alfRemaining!(selfAddress!()))
         |  }
         |}
         |""".stripMargin

    val contractId = createContract(input, 2, 2, AVector.empty).key

    val main =
      s"""
         |TxScript Main {
         |  pub payable fn main() -> () {
         |    let foo = Foo(#${contractId.toHexString})
         |    foo.foo(@${genesisAddress.toBase58})
         |  }
         |}
         |
         |$input
         |""".stripMargin

    val script = Compiler.compileTxScript(main).toOption.get
    fail(blockFlow, chainIndex, script, EmptyContractAsset)
  }

  it should "use latest worldstate when call external functions" in new ContractFixture {
    val input0 =
      s"""
         |TxContract Foo(mut x: U256) {
         |  pub fn get() -> (U256) {
         |    return x
         |  }
         |
         |  pub fn foo(foo: ByteVec, bar: ByteVec) -> () {
         |    x = x + 10
         |    x = Bar(bar).bar(foo)
         |    return
         |  }
         |}
         |
         |TxContract Bar(mut x: U256) {
         |  pub fn bar(foo: ByteVec) -> (U256) {
         |    return Foo(foo).get() + 100
         |  }
         |}
         |""".stripMargin
    val contractOutputRef0 = createContract(input0, 2, 2)
    val contractKey0       = contractOutputRef0.key

    val input1 =
      s"""
         |TxContract Bar(mut x: U256) {
         |  pub fn bar(foo: ByteVec) -> (U256) {
         |    return Foo(foo).get() + 100
         |  }
         |}
         |
         |TxContract Foo(mut x: U256) {
         |  pub fn get() -> (U256) {
         |    return x
         |  }
         |
         |  pub fn foo(foo: ByteVec, bar: ByteVec) -> () {
         |    x = x + 10
         |    x = Bar(bar).bar(foo)
         |    return
         |  }
         |}
         |
         |""".stripMargin
    val contractOutputRef1 = createContract(input1, 3, 3)
    val contractKey1       = contractOutputRef1.key

    val main =
      s"""
         |TxScript Main {
         |  pub fn main() -> () {
         |    let foo = Foo(#${contractKey0.toHexString})
         |    foo.foo(#${contractKey0.toHexString}, #${contractKey1.toHexString})
         |  }
         |}
         |
         |TxContract Foo(mut x: U256) {
         |  pub fn get() -> (U256) {
         |    return x
         |  }
         |
         |  pub fn foo(foo: ByteVec, bar: ByteVec) -> () {
         |    x = x + 10
         |    x = Bar(bar).bar(foo)
         |    return
         |  }
         |}
         |""".stripMargin
    val script   = Compiler.compileTxScript(main).toOption.get
    val newState = AVector[Val](Val.U256(U256.unsafe(110)))
    val block    = simpleScript(blockFlow, chainIndex, script)
    addAndCheck(blockFlow, block)

    val worldState = blockFlow.getBestPersistedWorldState(chainIndex.from).fold(throw _, identity)
    worldState.getContractStates().toOption.get.length is 3

    checkState(
      blockFlow,
      chainIndex,
      contractKey0,
      newState,
      contractOutputRef0,
      numAssets = 5,
      numContracts = 3
    )
  }

  it should "issue new token" in new ContractFixture {
    val input =
      s"""
         |TxContract Foo(mut x: U256) {
         |  pub payable fn foo() -> () {
         |    issueToken!(10000000)
         |  }
         |}
         |""".stripMargin
    val contractOutputRef = createContract(input, 2, 2)
    val contractKey       = contractOutputRef.key

    val main =
      s"""
         |TxScript Main {
         |  pub payable fn main() -> () {
         |    let foo = Foo(#${contractKey.toHexString})
         |    foo.foo()
         |  }
         |}
         |
         |$input
         |""".stripMargin
    val script = Compiler.compileTxScript(main).toOption.get

    val block0 = payableCall(blockFlow, chainIndex, script)
    addAndCheck(blockFlow, block0)

    val worldState0 = blockFlow.getBestPersistedWorldState(chainIndex.from).fold(throw _, identity)
    worldState0.getContractStates().toOption.get.length is 2
    worldState0.getContractOutputs(ByteString.empty, Int.MaxValue).rightValue.foreach {
      case (ref, output) =>
        if (ref != ContractOutputRef.forSMT) {
          output.tokens.head is (contractKey -> U256.unsafe(10000000))
        }
    }

    val block1 = payableCall(blockFlow, chainIndex, script)
    addAndCheck(blockFlow, block1)

    val worldState1 = blockFlow.getBestPersistedWorldState(chainIndex.from).fold(throw _, identity)
    worldState1.getContractStates().toOption.get.length is 2
    worldState1.getContractOutputs(ByteString.empty, Int.MaxValue).rightValue.foreach {
      case (ref, output) =>
        if (ref != ContractOutputRef.forSMT) {
          output.tokens.head is (contractKey -> U256.unsafe(20000000))
        }
    }
  }

  it should "use correct utxos" in new ContractFixture {
    val block = transfer(blockFlow, ChainIndex.unsafe(0, 1))
    addAndCheck(blockFlow, block)
    val input =
      s"""
         |TxContract Foo(mut x: U256) {
         |  pub payable fn foo() -> () {
         |    issueToken!(10000000)
         |  }
         |}
         |""".stripMargin
    createContract(input, 2, 2)
  }

  // scalastyle:off method.length
  it should "test operators" in new ContractFixture {
    // scalastyle:off no.equal
    def expect(out: Int) =
      s"""
         |TxScript Inverse {
         |  pub fn main() -> () {
         |    let x = 10973
         |    let mut y = 1
         |    let mut i = 0
         |    while (i <= 8) {
         |      y = y ⊗ (2 ⊖ x ⊗ y)
         |      i = i + 1
         |    }
         |    let r = x ⊗ y
         |    assert!(r == $out)
         |
         |    test()
         |  }
         |
         |  fn test() -> () {
         |    assert!((33 + 2 - 3) * 5 / 7 % 11 == 0)
         |
         |    let x = 0
         |    let y = 1
         |    assert!(x << 1 == 0)
         |    assert!(x >> 1 == 0)
         |    assert!(y << 1 == 2)
         |    assert!(y >> 1 == 0)
         |    assert!(y << 255 != 0)
         |    assert!(y << 256 == 0)
         |    assert!(x & x == 0)
         |    assert!(x & y == 0)
         |    assert!(y & y == 1)
         |    assert!(x | x == 0)
         |    assert!(x | y == 1)
         |    assert!(y | y == 1)
         |    assert!(x ^ x == 0)
         |    assert!(x ^ y == 1)
         |    assert!(y ^ y == 0)
         |
         |    assert!((x < y) == true)
         |    assert!((x <= y) == true)
         |    assert!((x < x) == false)
         |    assert!((x <= x) == true)
         |    assert!((x > y) == false)
         |    assert!((x >= y) == false)
         |    assert!((x > x) == false)
         |    assert!((x >= x) == true)
         |
         |    assert!((true && true) == true)
         |    assert!((true && false) == false)
         |    assert!((false && false) == false)
         |    assert!((true || true) == true)
         |    assert!((true || false) == true)
         |    assert!((false || false) == false)
         |
         |    assert!(!true == false)
         |    assert!(!false == true)
         |  }
         |}
         |""".stripMargin
    // scalastyle:on no.equal

    {
      val script = Compiler.compileTxScript(expect(1)).rightValue
      val block  = simpleScript(blockFlow, chainIndex, script)
      addAndCheck(blockFlow, block)
    }

    {
      val script = Compiler.compileTxScript(expect(2)).rightValue
      val block  = simpleScript(blockFlow, chainIndex, script)
      fail(blockFlow, block, AssertionFailed)
    }
  }
  // scalastyle:on method.length

  // scalastyle:off no.equal
  it should "test contract instructions" in new ContractFixture {
    def createContract(input: String): (String, String, String) = {
      val contractId    = createContract(input, initialState = AVector.empty).key
      val worldState    = blockFlow.getBestPersistedWorldState(chainIndex.from).rightValue
      val contractState = worldState.getContractState(contractId).rightValue
      val address       = Address.Contract(LockupScript.p2c(contractId)).toBase58
      (contractId.toHexString, address, contractState.code.hash.toHexString)
    }

    val foo =
      s"""
         |TxContract Foo() {
         |  pub fn foo(fooId: ByteVec, fooCodeHash: ByteVec, barId: ByteVec, barCodeHash: ByteVec, barAddress: Address) -> () {
         |    assert!(selfContractId!() == fooId)
         |    assert!(contractCodeHash!(fooId) == fooCodeHash)
         |    assert!(contractCodeHash!(barId) == barCodeHash)
         |    assert!(callerAddress!() == barAddress)
         |    assert!(callerCodeHash!() == barCodeHash)
         |    assert!(isCalledFromTxScript!() == false)
         |  }
         |}
         |""".stripMargin
    val (fooId, _, fooHash) = createContract(foo)

    val bar =
      s"""
         |TxContract Bar() {
         |  pub payable fn bar(fooId: ByteVec, fooCodeHash: ByteVec, barId: ByteVec, barCodeHash: ByteVec, barAddress: Address) -> () {
         |    assert!(selfContractId!() == barId)
         |    assert!(selfAddress!() == barAddress)
         |    assert!(contractCodeHash!(fooId) == fooCodeHash)
         |    assert!(contractCodeHash!(barId) == barCodeHash)
         |    Foo(#$fooId).foo(fooId, fooCodeHash, barId, barCodeHash, barAddress)
         |    assert!(isCalledFromTxScript!() == true)
         |    assert!(isPaying!(@$genesisAddress) == false)
         |  }
         |}
         |
         |$foo
         |""".stripMargin
    val (barId, barAddress, barHash) = createContract(bar)

    def main(state: String) =
      s"""
         |TxScript Main {
         |  pub payable fn main() -> () {
         |    Bar(#$barId).bar(#$fooId, #$fooHash, #$barId, #$barHash, @$barAddress)
         |    approveAlf!(@$genesisAddress, ${ALF.alf(1).v})
         |    copyCreateContract!(#$fooId, #$state)
         |    assert!(isPaying!(@$genesisAddress) == true)
         |  }
         |}
         |
         |$bar
         |""".stripMargin

    {
      val script = Compiler.compileTxScript(main("00")).rightValue
      val block  = payableCall(blockFlow, chainIndex, script)
      addAndCheck(blockFlow, block)
    }

    {
      info("Try to create a new contract with invalid number of fields")
      val script = Compiler.compileTxScript(main("010001")).rightValue
      fail(blockFlow, chainIndex, script, InvalidFieldLength)
    }
  }

  it should "destroy contract" in new ContractFixture {
    val foo =
      s"""
         |TxContract Foo() {
         |  pub fn foo() -> () {
         |    return
         |  }
         |}
         |""".stripMargin
    val fooId = createContract(foo, initialState = AVector.empty).key.toHexString

    def main(targetAddress: String) =
      s"""
         |TxScript Main {
         |  pub payable fn main() -> () {
         |    destroyContract!(#$fooId, @$targetAddress)
         |  }
         |}
         |""".stripMargin

    {
      info("Destroy a contract with contract address")
      val address = Address.Contract(LockupScript.P2C(Hash.generate))
      val script  = Compiler.compileTxScript(main(address.toBase58)).rightValue
      fail(blockFlow, chainIndex, script, InvalidAddressTypeInContractDestroy)
    }

    {
      info("Destroy a contract properly")
      val script = Compiler.compileTxScript(main(genesisAddress.toBase58)).rightValue
      val block  = payableCall(blockFlow, chainIndex, script)
      addAndCheck(blockFlow, block)
    }

    {
      info("Destroy a contract twice")
      val fooId = createContract(foo, initialState = AVector.empty).key.toHexString
      val main =
        s"""
           |TxScript Main {
           |  pub payable fn main() -> () {
           |    destroyContract!(#$fooId, @${genesisAddress.toBase58})
           |    destroyContract!(#$fooId, @${genesisAddress.toBase58})
           |  }
           |}
           |""".stripMargin
      val script = Compiler.compileTxScript(main).rightValue
      intercept[AssertionError](payableCall(blockFlow, chainIndex, script)).getMessage
        .startsWith("Left(org.alephium.io.IOError$KeyNotFound") is true
    }
  }

  it should "fetch block env" in new ContractFixture {
    def main(latestHeader: BlockHeader) =
      s"""
         |TxScript Main {
         |  pub fn main() -> () {
         |    assert!(chainId!() == #02)
         |    assert!(blockTimeStamp!() >= ${latestHeader.timestamp.millis})
         |    assert!(blockTarget!() == ${latestHeader.target.value})
         |  }
         |}
         |""".stripMargin

    def test() = {
      val latestTip    = blockFlow.getHeaderChain(chainIndex).getBestTipUnsafe()
      val latestHeader = blockFlow.getBlockHeaderUnsafe(latestTip)
      val script       = Compiler.compileTxScript(main(latestHeader)).rightValue
      val block        = simpleScript(blockFlow, chainIndex, script)
      addAndCheck(blockFlow, block)
    }

    // we test with three new blocks
    test()
    test()
    test()
  }

  // scalastyle:off regex
  it should "test hash built-ins" in new ContractFixture {
    val input = Hex.toHexString(ByteString.fromString("Hello World1"))
    val main =
      s"""
         |TxScript Main {
         |  pub fn main() -> () {
         |    assert!(blake2b!(#$input) == #8947bee8a082f643a8ceab187d866e8ec0be8c2d7d84ffa8922a6db77644b37a)
         |    assert!(blake2b!(#$input) != #8947bee8a082f643a8ceab187d866e8ec0be8c2d7d84ffa8922a6db77644b370)
         |    assert!(keccak256!(#$input) == #2744686CE50A2A5AE2A94D18A3A51149E2F21F7EEB4178DE954A2DFCADC21E3C)
         |    assert!(keccak256!(#$input) != #2744686CE50A2A5AE2A94D18A3A51149E2F21F7EEB4178DE954A2DFCADC21E30)
         |    assert!(sha256!(#$input) == #6D1103674F29502C873DE14E48E9E432EC6CF6DB76272C7B0DAD186BB92C9A9A)
         |    assert!(sha256!(#$input) != #6D1103674F29502C873DE14E48E9E432EC6CF6DB76272C7B0DAD186BB92C9A90)
         |    assert!(sha3!(#$input) == #f5ad69e6b85ae4a51264df200c2bd19fbc337e4160c77dfaa1ea98cbae8ed743)
         |    assert!(sha3!(#$input) != #f5ad69e6b85ae4a51264df200c2bd19fbc337e4160c77dfaa1ea98cbae8ed740)
         |  }
         |}
         |""".stripMargin
    testSimpleScript(main)
  }

  // scalastyle:off no.equal
  it should "test signature built-ins" in new ContractFixture {
    val zero                     = Hash.zero.toHexString
    val (p256Pri, p256Pub)       = SecP256K1.generatePriPub()
    val p256Sig                  = SecP256K1.sign(Hash.zero.bytes, p256Pri).toHexString
    val (ed25519Pri, ed25519Pub) = ED25519.generatePriPub()
    val ed25519Sig               = ED25519.sign(Hash.zero.bytes, ed25519Pri).toHexString
    val main =
      s"""
         |TxScript Main {
         |  pub fn main() -> () {
         |    assert!(verifySecP256K1!(#$zero, #${p256Pub.toHexString}, #$p256Sig) == true)
         |    assert!(verifySecP256K1!(#$zero, #${p256Pub.toHexString}, #${SecP256K1Signature.zero.toHexString}) == false)
         |    assert!(verifyED25519!(#$zero, #${ed25519Pub.toHexString}, #$ed25519Sig) == true)
         |    assert!(verifyED25519!(#$zero, #${ed25519Pub.toHexString}, #${ED25519Signature.zero.toHexString}) == false)
         |  }
         |}
         |""".stripMargin
    testSimpleScript(main)
  }

  behavior of "constant product market"

  it should "swap" in new ContractFixture {
    val tokenContract =
      s"""
         |TxContract Token(mut x: U256) {
         |  pub payable fn issue(amount: U256) -> () {
         |    issueToken!(amount)
         |  }
         |
         |  pub payable fn withdraw(address: Address, amount: U256) -> () {
         |    transferTokenFromSelf!(address, selfTokenId!(), amount)
         |  }
         |}
         |""".stripMargin
    val tokenContractKey = createContract(tokenContract, 2, 2).key
    val tokenId          = tokenContractKey

    callTxScript(s"""
                    |TxScript Main {
                    |  pub payable fn main() -> () {
                    |    let token = Token(#${tokenContractKey.toHexString})
                    |    token.issue(1024)
                    |  }
                    |}
                    |
                    |$tokenContract
                    |""".stripMargin)

    callTxScript(s"""
                    |TxScript Main {
                    |  pub payable fn main() -> () {
                    |    let token = Token(#${tokenContractKey.toHexString})
                    |    token.withdraw(@${genesisAddress.toBase58}, 1024)
                    |  }
                    |}
                    |
                    |$tokenContract
                    |""".stripMargin)

    val swapContract =
      s"""
         |// Simple swap contract purely for testing
         |
         |TxContract Swap(tokenId: ByteVec, mut alfReserve: U256, mut tokenReserve: U256) {
         |  pub payable fn setup() -> () {
         |    issueToken!(10000)
         |  }
         |
         |  pub payable fn addLiquidity(lp: Address, alfAmount: U256, tokenAmount: U256) -> () {
         |    transferAlfToSelf!(lp, alfAmount)
         |    transferTokenToSelf!(lp, tokenId, tokenAmount)
         |    alfReserve = alfAmount
         |    tokenReserve = tokenAmount
         |  }
         |
         |  pub payable fn swapToken(buyer: Address, alfAmount: U256) -> () {
         |    let tokenAmount = tokenReserve - alfReserve * tokenReserve / (alfReserve + alfAmount)
         |    transferAlfToSelf!(buyer, alfAmount)
         |    transferTokenFromSelf!(buyer, tokenId, tokenAmount)
         |    alfReserve = alfReserve + alfAmount
         |    tokenReserve = tokenReserve - tokenAmount
         |  }
         |
         |  pub payable fn swapAlf(buyer: Address, tokenAmount: U256) -> () {
         |    let alfAmount = alfReserve - alfReserve * tokenReserve / (tokenReserve + tokenAmount)
         |    transferTokenToSelf!(buyer, tokenId, tokenAmount)
         |    transferAlfFromSelf!(buyer, alfAmount)
         |    alfReserve = alfReserve - alfAmount
         |    tokenReserve = tokenReserve + tokenAmount
         |  }
         |}
         |""".stripMargin
    val swapContractKey = createContract(
      swapContract,
      AVector[Val](Val.ByteVec.from(tokenId), Val.U256(U256.Zero), Val.U256(U256.Zero))
    ).key

    def checkSwapBalance(alfReserve: U256, tokenReserve: U256) = {
      val worldState = blockFlow.getBestPersistedWorldState(chainIndex.from).fold(throw _, identity)
      val output     = worldState.getContractAsset(swapContractKey).toOption.get
      output.amount is alfReserve
      output.tokens.toSeq.toMap.getOrElse(tokenId, U256.Zero) is tokenReserve
    }

    callTxScript(s"""
                    |TxScript Main {
                    |  pub payable fn main() -> () {
                    |    let swap = Swap(#${swapContractKey.toHexString})
                    |    swap.setup()
                    |  }
                    |}
                    |
                    |$swapContract
                    |""".stripMargin)
    checkSwapBalance(dustUtxoAmount, 0)

    callTxScript(s"""
                    |TxScript Main {
                    |  pub payable fn main() -> () {
                    |    approveAlf!(@${genesisAddress.toBase58}, 10)
                    |    approveToken!(@${genesisAddress.toBase58}, #${tokenId.toHexString}, 100)
                    |    let swap = Swap(#${swapContractKey.toHexString})
                    |    swap.addLiquidity(@${genesisAddress.toBase58}, 10, 100)
                    |  }
                    |}
                    |
                    |$swapContract
                    |""".stripMargin)
    checkSwapBalance(dustUtxoAmount + 10, 100)

    callTxScript(s"""
                    |TxScript Main {
                    |  pub payable fn main() -> () {
                    |    approveAlf!(@${genesisAddress.toBase58}, 10)
                    |    let swap = Swap(#${swapContractKey.toHexString})
                    |    swap.swapToken(@${genesisAddress.toBase58}, 10)
                    |  }
                    |}
                    |
                    |$swapContract
                    |""".stripMargin)
    checkSwapBalance(dustUtxoAmount + 20, 50)

    callTxScript(s"""
                    |TxScript Main {
                    |  pub payable fn main() -> () {
                    |    approveToken!(@${genesisAddress.toBase58}, #${tokenId.toHexString}, 50)
                    |    let swap = Swap(#${swapContractKey.toHexString})
                    |    swap.swapAlf(@${genesisAddress.toBase58}, 50)
                    |  }
                    |}
                    |
                    |$swapContract
                    |""".stripMargin)
    checkSwapBalance(dustUtxoAmount + 10, 100)
  }

  behavior of "random execution"

  it should "execute tx in random order" in new ContractFixture {
    val testContract =
      s"""
         |TxContract Foo(mut x: U256) {
         |  pub fn foo(y: U256) -> () {
         |    x = x * 10 + y
         |  }
         |}
         |""".stripMargin
    val contractKey = createContract(testContract, 2, 2).key

    val block = callTxScriptMulti(index => s"""
         |TxScript Main {
         |  pub fn main() -> () {
         |    let foo = Foo(#${contractKey.toHexString})
         |    foo.foo($index)
         |  }
         |}
         |
         |$testContract
         |""".stripMargin)

    val expected      = block.getNonCoinbaseExecutionOrder.fold(0L)(_ * 10 + _)
    val worldState    = blockFlow.getBestPersistedWorldState(chainIndex.from).fold(throw _, identity)
    val contractState = worldState.getContractState(contractKey).fold(throw _, identity)
    contractState.fields is AVector[Val](Val.U256(U256.unsafe(expected)))
  }
}
// scalastyle:on file.size.limit no.equal regex
