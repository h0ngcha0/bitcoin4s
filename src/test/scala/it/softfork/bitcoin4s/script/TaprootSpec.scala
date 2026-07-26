package it.softfork.bitcoin4s.script

import scala.io.Source

import play.api.libs.json.{Json, OFormat}
import scodec.Attempt
import scodec.bits.BitVector

import it.softfork.bitcoin4s.Spec
import it.softfork.bitcoin4s.crypto.{Schnorr, Secp256k1}
import it.softfork.bitcoin4s.script.ConstantOp.OP_PUSHDATA
import it.softfork.bitcoin4s.script.InterpreterError._
import it.softfork.bitcoin4s.script.LocktimeOp.{OP_CHECKLOCKTIMEVERIFY, OP_CHECKSEQUENCEVERIFY}
import it.softfork.bitcoin4s.script.SigVersion.{
  SIGVERSION_BASE,
  SIGVERSION_TAPROOT,
  SIGVERSION_TAPSCRIPT
}
import it.softfork.bitcoin4s.transaction.{OutPoint, Script, Tx, TxIn, TxOut}
import it.softfork.bitcoin4s.transaction.structure.{Hash => TransactionHash, VarInt}
import it.softfork.bitcoin4s.utils._

// Official vectors pinned at:
// https://github.com/bitcoin/bips/blob/b289d016b99c81527623c10e995e0318f744ebf3/bip-0340/test-vectors.csv
// https://github.com/bitcoin/bips/blob/b289d016b99c81527623c10e995e0318f744ebf3/bip-0341/wallet-test-vectors.json
// https://github.com/bitcoin-core/qa-assets/blob/6ea645d13b09bb5be644f154e50a9aa84f929ecb/unit_test_data/script_assets_test.json
// The exact qa-assets selection and checksums are in scripts/update-taproot-script-assets.sh.
class TaprootSpec extends Spec {
  case class Spend(scriptSig: String, witness: Seq[String])
  case class ScriptAsset(
    tx: String,
    prevouts: Seq[String],
    index: Int,
    success: Spend,
    failure: Option[Spend],
    flags: String,
    comment: String
  )

  implicit val spendFormat: OFormat[Spend] = Json.format[Spend]
  implicit val scriptAssetFormat: OFormat[ScriptAsset] = Json.format[ScriptAsset]

  private val locktimeThreshold = 500000000L

  "Schnorr" should "pass every official BIP340 verification vector" in {
    val resource = getClass.getResource("/bip340-test-vectors.csv")
    val rows = Source.fromURI(resource.toURI).getLines().drop(1)

    rows.foreach { row =>
      val fields = row.split(",", -1)
      val index = fields(0)
      val result = Schnorr.verify(
        hexToBytes(fields(2)).toIndexedSeq,
        hexToBytes(fields(4)).toIndexedSeq,
        hexToBytes(fields(5)).toIndexedSeq
      )

      withClue(s"BIP340 vector $index") {
        result shouldEqual fields(6).toBoolean
      }
    }
  }

  "CompactSize" should "encode every Taproot boundary canonically" in {
    val vectors = Seq(
      252L -> "fc",
      253L -> "fdfd00",
      65535L -> "fdffff",
      65536L -> "fe00000100"
    )

    vectors.foreach { case (value, expected) =>
      withClue(s"CompactSize $value") {
        VarInt.varIntCodec.encode(value).toOption.value.toHex shouldEqual expected
      }
    }
  }

  it should "commit and budget a 65,535-byte TapLeaf canonically" in {
    val script = Array.fill[Byte](65535)(0x61)
    val expectedTapLeafHash =
      "e8c51e124e2048232fda982f65d00d75922e12a59013a5ec14c84abda9cd2182"

    Taproot
      .tapLeafHash(0xc0, script.toIndexedSeq)
      .toIndexedSeq
      .toHex shouldEqual expectedTapLeafHash
    Taproot.serializedWitnessSize(Seq(ScriptConstant(script))) shouldEqual 65539
  }

  "Script flags" should "not advertise unimplemented CONST_SCRIPTCODE validation" in {
    ScriptFlag.fromString("CONST_SCRIPTCODE") shouldEqual None
    ScriptFlag.fromString("SCRIPT_VERIFY_CONST_SCRIPTCODE") shouldEqual None
  }

  "Taproot" should "derive the official BIP341 output key" in {
    val internalPublicKey =
      hexToBytes("d6889cb081036e0faefa3a35157ad71086b123b2b144b649798b494c300a961d")
    val expected =
      "53a1f6e454df1aa2776a2814a721372d6258050de330b3c6d10ee8f4e0dda343"

    val (outputKey, _) =
      Taproot.outputKey(internalPublicKey.toIndexedSeq, Seq.empty).value

    outputKey.toHex shouldEqual expected
  }

  it should "match every official BIP341 key-path signature hash" in {
    val transaction = Tx.fromHex(
      "02000000097de20cbff686da83a54981d2b9bab3586f4ca7e48f57f5b55963115f3b334e9c010000000000000000d7b7cab57b1393ace2d064f4d4a2cb8af6def61273e127517d44759b6dafdd990000000000fffffffff8e1f583384333689228c5d28eac13366be082dc57441760d957275419a418420000000000fffffffff0689180aa63b30cb162a73c6d2a38b7eeda2a83ece74310fda0843ad604853b0100000000feffffffaa5202bdf6d8ccd2ee0f0202afbbb7461d9264a25e5bfd3c5a52ee1239e0ba6c0000000000feffffff956149bdc66faa968eb2be2d2faa29718acbfe3941215893a2a3446d32acd050000000000000000000e664b9773b88c09c32cb70a2a3e4da0ced63b7ba3b22f848531bbb1d5d5f4c94010000000000000000e9aa6b8e6c9de67619e6a3924ae25696bb7b694bb677a632a74ef7eadfd4eabf0000000000ffffffffa778eb6a263dc090464cd125c466b5a99667720b1c110468831d058aa1b82af10100000000ffffffff0200ca9a3b000000001976a91406afd46bcdfd22ef94ac122aa11f241244a37ecc88ac807840cb0000000020ac9a87f5594be208f8532db38cff670c450ed2fea8fcdefcc9a663f78bab962b0065cd1d"
    )
    val spentOutputs = Seq(
      TxOut(
        420000000,
        Script("512053a1f6e454df1aa2776a2814a721372d6258050de330b3c6d10ee8f4e0dda343")
      ),
      TxOut(
        462000000,
        Script("5120147c9c57132f6e7ecddba9800bb0c4449251c92a1e60371ee77557b6620f3ea3")
      ),
      TxOut(294000000, Script("76a914751e76e8199196d454941c45d1b3a323f1433bd688ac")),
      TxOut(
        504000000,
        Script("5120e4d810fd50586274face62b8a807eb9719cef49c04177cc6b76a9a4251d5450e")
      ),
      TxOut(
        630000000,
        Script("512091b64d5324723a985170e4dc5a0f84c041804f2cd12660fa5dec09fc21783605")
      ),
      TxOut(378000000, Script("00147dd65592d0ab2fe0d0257d571abf032cd9db93dc")),
      TxOut(
        672000000,
        Script("512075169f4001aa68f15bbed28b218df1d0a62cbbcf1188c6665110c293c907b831")
      ),
      TxOut(
        546000000,
        Script("5120712447206d7a5238acc7ff53fbe94a3b64539ad291c7cdbc490b7577e4b17df5")
      ),
      TxOut(
        588000000,
        Script("512077e30a5522dd9f894c3f8b8bd4c4b2cf82ca7da8a3ea6a239655c39c050ab220")
      )
    )
    val vectors = Seq(
      (0, 3, "2514a6272f85cfa0f45eb907fcb0d121b808ed37c6ea160a5a9046ed5526d555"),
      (1, 0x83, "325a644af47e8a5a2591cda0ab0723978537318f10e6a63d4eed783b96a71a4d"),
      (3, 1, "bf013ea93474aa67815b1b6cc441d23b64fa310911d991e713cd34c7f5d46669"),
      (4, 0, "4f900a0bae3f1446fd48490c2958b5a023228f01661cda3496a11da502a7f7ef"),
      (6, 2, "15f25c298eb5cdc7eb1d638dd2d45c97c4c59dcaec6679cfc16ad84f30876b85"),
      (7, 0x82, "cd292de50313804dabe4685e83f923d2969577191a3e1d2882220dca88cbeb10"),
      (8, 0x81, "cccb739eca6c13a8a89e6e5cd317ffe55669bbda23f2fd37b0f18755e008edd2")
    )

    vectors.foreach { case (inputIndex, hashType, expected) =>
      val spentOutput = spentOutputs(inputIndex)
      val state = InterpreterState(
        scriptPubKey = Parser.parse(spentOutput.pk_script.value.toIndexedSeq),
        scriptSig = Seq.empty,
        scriptWitnessStack = None,
        flags = Seq(ScriptFlag.SCRIPT_VERIFY_TAPROOT),
        transaction = transaction,
        inputIndex = inputIndex,
        amount = spentOutput.value,
        spentOutputs = spentOutputs,
        sigVersion = SIGVERSION_BASE
      )

      val result = Taproot
        .signatureHash(
          state,
          hashType,
          SIGVERSION_TAPROOT,
          annex = None,
          tapLeafHash = None,
          codeSeparatorPosition = 0xffffffffL
        )
        .toOption
        .value

      withClue(s"BIP341 input $inputIndex, hash type $hashType") {
        result.toIndexedSeq.toHex shouldEqual expected
      }
    }
  }

  it should "inherit consensus CLTV and CSV semantics while preserving their operands" in {
    withClue("CLTV accepts equality and preserves its operand") {
      runLocktimeScript(
        comparisonScript(500, OP_CHECKLOCKTIMEVERIFY.value),
        version = 2,
        sequence = 0xfffffffeL,
        lockTime = 500
      ).toOption.value._2 shouldEqual Some(true)
    }

    withClue("CLTV rejects a locktime type mismatch") {
      runLocktimeScript(
        comparisonScript(locktimeThreshold, OP_CHECKLOCKTIMEVERIFY.value),
        version = 2,
        sequence = 0xfffffffeL,
        lockTime = locktimeThreshold - 1
      ).left.toOption.value shouldBe a[CLTVFailed]
    }

    withClue("CLTV rejects a final input") {
      runLocktimeScript(
        comparisonScript(500, OP_CHECKLOCKTIMEVERIFY.value),
        version = 2,
        sequence = 0xffffffffL,
        lockTime = 500
      ).left.toOption.value shouldBe a[CLTVFailed]
    }

    withClue("CSV rejects transaction versions below two") {
      runLocktimeScript(
        comparisonScript(5, OP_CHECKSEQUENCEVERIFY.value),
        version = 1,
        sequence = 5,
        lockTime = 0
      ).left.toOption.value shouldBe a[CSVFailed]
    }

    withClue("CSV rejects a disabled input sequence") {
      runLocktimeScript(
        comparisonScript(5, OP_CHECKSEQUENCEVERIFY.value),
        version = 2,
        sequence = 0x80000005L,
        lockTime = 0
      ).left.toOption.value shouldBe a[CSVFailed]
    }

    withClue("CSV treats a disabled operand as a stack-preserving NOP") {
      runLocktimeScript(
        comparisonScript(0x80000000L, OP_CHECKSEQUENCEVERIFY.value),
        version = 1,
        sequence = 0xffffffffL,
        lockTime = 0
      ).toOption.value._2 shouldEqual Some(true)
    }

    withClue("CSV compares masked values and accepts equality") {
      runLocktimeScript(
        comparisonScript(5, OP_CHECKSEQUENCEVERIFY.value),
        version = 2,
        sequence = 0x00010005L,
        lockTime = 0
      ).toOption.value._2 shouldEqual Some(true)
    }

    withClue("CSV rejects a larger masked operand") {
      runLocktimeScript(
        comparisonScript(6, OP_CHECKSEQUENCEVERIFY.value),
        version = 2,
        sequence = 5,
        lockTime = 0
      ).left.toOption.value shouldBe a[CSVFailed]
    }

    withClue("CSV rejects a sequence type mismatch") {
      runLocktimeScript(
        comparisonScript(0x00400005L, OP_CHECKSEQUENCEVERIFY.value),
        version = 2,
        sequence = 5,
        lockTime = 0
      ).left.toOption.value shouldBe a[CSVFailed]
    }
  }

  it should "expose each Tapscript step to the interpreter" in {
    val script = comparisonScript(500, OP_CHECKLOCKTIMEVERIFY.value)
    def run(steps: Int) =
      runLocktimeScript(
        script,
        version = 2,
        sequence = 0xfffffffeL,
        lockTime = 500,
        maybeSteps = Some(steps)
      ).toOption.value

    val before = run(4)
    val afterPush = run(5)

    before._2 shouldEqual None
    before._1.sigVersion shouldEqual SIGVERSION_TAPSCRIPT
    before._1.currentScript should have size 6
    afterPush._2 shouldEqual None
    afterPush._1.stack.head.bytes shouldEqual ScriptNum(500).bytes
    afterPush._1.currentScript should have size 4
    run(9)._2 shouldEqual Some(true)
  }

  it should "execute the pinned 30,005-opcode Tapscript without a script-size limit" in {
    val asset = loadScriptAssets().find(_.comment == "tapscript/no10000limit").value
    val script = hexToBytes(asset.success.witness(asset.success.witness.length - 2))

    Parser
      .parse(script.toIndexedSeq)
      .count(!_.isInstanceOf[ScriptConstant]) shouldEqual 30005
    runAsset(asset, asset.success).toOption.value._2 shouldEqual Some(true)
  }

  it should "execute the selected Bitcoin Core Taproot script assets" in {
    val assets = loadScriptAssets()
    val expectedFailures = Map[Class[_ <: InterpreterError], Seq[String]](
      classOf[InvalidStackOperation] -> Seq("tapscript/checksigadd3args"),
      classOf[GeneralError] -> Seq("tapscript/checksigaddoversize"),
      classOf[TapscriptMinimalIf] -> Seq(
        "tapscript/minimalif",
        "tapscript/minimalnotif"
      ),
      classOf[TapscriptCheckMultiSig] -> Seq(
        "tapscript/disabled_checkmultisig",
        "tapscript/disabled_checkmultisigverify"
      ),
      classOf[TapscriptValidationWeight] -> Seq("tapscript/sigopsratio_0"),
      classOf[ExceedMaxStackSize] -> Seq(
        "tapscript/1000inputs",
        "tapscript/1000stack"
      ),
      classOf[ExceedMaxPushSize] -> Seq(
        "tapscript/inputmaxlimit",
        "tapscript/pushmaxlimit"
      ),
      classOf[RequireCleanStack] -> Seq("tapscript/cleanstack"),
      classOf[SchnorrSignatureVerificationFailed] -> Seq("sighash/codesep_pk"),
      classOf[TapscriptEmptyPublicKey] -> Seq("tapscript/emptypk/checksig")
    ).flatMap { case (errorClass, comments) =>
      comments.map(_ -> errorClass)
    }

    assets.foreach { asset =>
      val successResult = runAsset(asset, asset.success)
      withClue(s"Bitcoin Core script asset ${asset.comment}, success: $successResult") {
        successResult.toOption.value._2 shouldEqual Some(true)
      }

      asset.failure.foreach { failure =>
        withClue(s"Bitcoin Core script asset ${asset.comment}, failure") {
          val result = runAsset(asset, failure)
          expectedFailures.get(asset.comment).foreach { expected =>
            result.left.toOption.value.getClass shouldEqual expected
          }
          result.toOption.map(_._2) should not contain Some(true)
        }
      }
    }

    val opSuccess = assets.find(_.comment == "opsuccess/bare").value
    runAsset(
      opSuccess,
      opSuccess.success,
      Seq(ScriptFlag.SCRIPT_VERIFY_DISCOURAGE_OP_SUCCESS)
    ).left.toOption.value shouldBe a[DiscourageOpSuccess]

    val unknownPublicKey = assets.find(_.comment == "tapscript/unkpk/checksigadd").value
    runAsset(
      unknownPublicKey,
      unknownPublicKey.success,
      Seq(ScriptFlag.SCRIPT_VERIFY_DISCOURAGE_UPGRADABLE_PUBKEYTYPE)
    ).left.toOption.value shouldBe a[DiscourageUpgradablePublicKeyType]

    val unknownLeaf = assets.find(_.comment == "inactive/scriptpath_valid_unkleaf").value
    runAsset(
      unknownLeaf,
      unknownLeaf.success,
      Seq(ScriptFlag.SCRIPT_VERIFY_TAPROOT)
    ).toOption.value._2 shouldEqual Some(true)
    runAsset(
      unknownLeaf,
      unknownLeaf.success,
      Seq(
        ScriptFlag.SCRIPT_VERIFY_TAPROOT,
        ScriptFlag.SCRIPT_VERIFY_DISCOURAGE_UPGRADABLE_TAPROOT_VERSION
      )
    ).left.toOption.value shouldBe a[DiscourageUpgradableTaprootVersion]
  }

  private def runAsset(
    asset: ScriptAsset,
    spend: Spend,
    additionalFlags: Seq[ScriptFlag] = Seq.empty
  ): Either[InterpreterError, (InterpreterState, Option[Boolean])] = {
    val spentOutputs = asset.prevouts.map(decodeOutput)
    val flags = asset.flags
      .split(",")
      .flatMap(flag => ScriptFlag.fromString(flag).toList)
      .toIndexedSeq ++ additionalFlags
    val transaction = Tx.fromHex(asset.tx)
    val input = transaction.tx_in(asset.index)
    val updatedTransaction = transaction.copy(
      tx_in = transaction.tx_in.updated(
        asset.index,
        input.copy(sig_script = Script(spend.scriptSig))
      )
    )
    val spentOutput = spentOutputs(asset.index)
    val witness = spend.witness.reverse
      .map(value => ScriptConstant(hexToBytes(value)))

    val state = InterpreterState(
      scriptPubKey = Parser.parse(spentOutput.pk_script.value.toIndexedSeq),
      scriptSig = Parser.parse(hexToBytes(spend.scriptSig).toIndexedSeq),
      scriptWitnessStack = Some(witness),
      flags = flags,
      transaction = updatedTransaction,
      inputIndex = asset.index,
      amount = spentOutput.value,
      spentOutputs = spentOutputs,
      sigVersion = SIGVERSION_BASE
    )

    Interpreter.create(verbose = false).run(state)
  }

  private def loadScriptAssets(): Seq[ScriptAsset] = {
    val resource = getClass.getResource("/taproot-script-assets.json")
    Json.parse(Source.fromURI(resource.toURI).mkString).as[Seq[ScriptAsset]]
  }

  private def comparisonScript(value: Long, locktimeOp: Long): Array[Byte] = {
    val encoded = ScriptNum(value).bytes.toArray
    val push = Array(OP_PUSHDATA(encoded.length.toLong).value.toByte) ++ encoded
    push ++ Array(locktimeOp.toByte) ++ push ++ Array(BitwiseLogicOp.OP_EQUAL.value.toByte)
  }

  private def runLocktimeScript(
    script: Array[Byte],
    version: Long,
    sequence: Long,
    lockTime: Long,
    maybeSteps: Option[Int] = None
  ): Either[InterpreterError, (InterpreterState, Option[Boolean])] = {
    val internalPublicKey = Secp256k1.params.getG.getEncoded(true).tail.toIndexedSeq
    val leafHash = Taproot.tapLeafHash(0xc0, script.toIndexedSeq)
    val (outputKey, odd) =
      Taproot.outputKey(internalPublicKey, leafHash.toIndexedSeq).value
    val control =
      Array((0xc0 | odd.option(1).getOrElse(0)).toByte) ++ internalPublicKey
    val scriptPubKeyBytes =
      Array(ConstantOp.OP_1.value.toByte, OP_PUSHDATA(32).value.toByte) ++ outputKey
    val spentOutput = TxOut(1, Script(scriptPubKeyBytes))
    val transaction = Tx(
      version = version,
      flag = false,
      tx_in = List(
        TxIn(
          OutPoint(TransactionHash.NULL, 0),
          Script.empty,
          sequence
        )
      ),
      tx_out = List(TxOut(0, Script.empty)),
      tx_witness = List.empty,
      lock_time = lockTime
    )
    val state = InterpreterState(
      scriptPubKey = Parser.parse(scriptPubKeyBytes.toIndexedSeq),
      scriptSig = Seq.empty,
      scriptWitnessStack = Some(Seq(ScriptConstant(control), ScriptConstant(script))),
      flags = Seq(
        ScriptFlag.SCRIPT_VERIFY_WITNESS,
        ScriptFlag.SCRIPT_VERIFY_TAPROOT,
        ScriptFlag.SCRIPT_VERIFY_CHECKLOCKTIMEVERIFY,
        ScriptFlag.SCRIPT_VERIFY_CHECKSEQUENCEVERIFY
      ),
      transaction = transaction,
      inputIndex = 0,
      amount = spentOutput.value,
      spentOutputs = Seq(spentOutput),
      sigVersion = SIGVERSION_BASE
    )

    Interpreter.create(verbose = false, maybeSteps).run(state)
  }

  private def decodeOutput(hex: String): TxOut = {
    TxOut.codec.decodeValue(BitVector(hexToBytes(hex))) match {
      case Attempt.Successful(output) =>
        output
      case Attempt.Failure(error) =>
        fail(error.messageWithContext)
    }
  }
}
