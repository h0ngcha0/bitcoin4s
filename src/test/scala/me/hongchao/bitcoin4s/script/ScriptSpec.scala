package me.hongchao.bitcoin4s.script

import com.typesafe.config._

import scala.collection.JavaConverters._
import me.hongchao.bitcoin4s.Spec
import me.hongchao.bitcoin4s.Utils._
import org.spongycastle.util.encoders.Hex

import scala.io.Source
import scala.util.control.Exception.allCatch

class ScriptSpec extends Spec with ScriptTestRunner {

  implicit class RichConfigValue(configValue: ConfigValue) {
    def toList(): List[ConfigValue] = {
      configValue.asInstanceOf[ConfigList].iterator().asScala.toList
    }
  }

  val testConfigString = Source.fromURI(getClass.getResource("/script_test.json").toURI).mkString
  val testConfig: Config = ConfigFactory.parseString(
    s"""
       |bitcoin4s {
       |  script_tests = $testConfigString
       |}
     """.stripMargin)

  "Interpreter" should "pass script_test.json in bitcoin reference client code base" in {
    val scriptTestsConfig: List[List[ConfigValue]] = testConfig
      .getList("bitcoin4s.script_tests")
      .toList
      .map(_.toList)

    val rawScriptTests = scriptTestsConfig
      .filter(_.length > 3)

    lazy val scriptTests = rawScriptTests.collect {
      case elements @ (head :: tail)  =>
        if (head.isInstanceOf[ConfigList]) {
          val witnessElement = head.toList.map(_.render)
          val amount = (BigDecimal(witnessElement.last) * 100000000).toLong
          val stringTail = tail.map(stripDoubleQuotes)
          val comments = (stringTail.length == 5).option(stringTail.last).getOrElse("")
          val witnesses = witnessElement.reverse.tail.flatMap { rawWitness =>
            allCatch.opt(Hex.decode(stripDoubleQuotes(rawWitness)).toSeq)
          }.map(ScriptConstant.apply)
          val List(scriptSigString, scriptPubKeyString, scriptFlagsString, expectedResultString) = stringTail.take(4)
          val scriptFlags = toScriptFlags(scriptFlagsString)
          val expectedResult = ExpectedResult.fromString(expectedResultString).value

          TestCase(
            scriptSig = Parser.parse(scriptSigString),
            scriptPubKey = Parser.parse(scriptPubKeyString),
            scriptFlags = scriptFlags,
            expectedResult = expectedResult,
            comments = comments,
            witness = Some((witnesses, amount)),
            raw = elements.toString
          )
        } else {
          val stringElements = elements.map(stripDoubleQuotes)
          val List(scriptSigString, scriptPubKeyString, scriptFlagsString, expectedResultString) = stringElements.take(4)
          val expectedResult = ExpectedResult.fromString(expectedResultString).value
          val comments = (stringElements.length == 5).option(stringElements.last).getOrElse("")
          val scriptFlags = toScriptFlags(scriptFlagsString)
          val scriptSig = Parser.parse(scriptSigString)
          val scriptPubKey = Parser.parse(scriptPubKeyString)

          TestCase(
            scriptSig = scriptSig,
            scriptPubKey = scriptPubKey,
            scriptFlags = scriptFlags,
            expectedResult = expectedResult,
            comments = comments,
            witness = None,
            raw = elements.toString
          )
        }
    }

    val checkedExpectedResults = Seq(
      ExpectedResult.OK,
      ExpectedResult.EVAL_FALSE,
      ExpectedResult.BAD_OPCODE,
      ExpectedResult.CLEANSTACK,
      ExpectedResult.DISABLED_OPCODE,
      ExpectedResult.DISCOURAGE_UPGRADABLE_NOPS,
      ExpectedResult.EQUALVERIFY,
      ExpectedResult.INVALID_ALTSTACK_OPERATION,
      ExpectedResult.INVALID_STACK_OPERATION,
      ExpectedResult.MINIMALDATA,
      ExpectedResult.UNBALANCED_CONDITIONAL,
      ExpectedResult.NEGATIVE_LOCKTIME,
      ExpectedResult.OP_COUNT,
      ExpectedResult.OP_RETURN,
      ExpectedResult.VERIFY,
      ExpectedResult.PUSH_SIZE,
      ExpectedResult.STACK_SIZE,
      ExpectedResult.SCRIPT_SIZE,
      ExpectedResult.PUBKEY_COUNT,
      ExpectedResult.SIG_COUNT,
      ExpectedResult.SIG_PUSHONLY,
      ExpectedResult.PUBKEYTYPE,
      ExpectedResult.SIG_DER,
      ExpectedResult.NULLFAIL,
      ExpectedResult.SIG_NULLDUMMY,
      ExpectedResult.WITNESS_PROGRAM_MISMATCH,
      ExpectedResult.WITNESS_PROGRAM_WRONG_LENGTH,
      ExpectedResult.WITNESS_PROGRAM_WITNESS_EMPTY,
      ExpectedResult.SIG_HIGH_S,
      ExpectedResult.SIG_HASHTYPE,
      ExpectedResult.DISCOURAGE_UPGRADABLE_WITNESS_PROGRAM,
      ExpectedResult.MINIMALIF,
      ExpectedResult.UNSATISFIED_LOCKTIME,
      ExpectedResult.WITNESS_PUBKEYTYPE,
      ExpectedResult.WITNESS_UNEXPECTED
    )

    val notCheckedExpectedResults = Seq(
      ExpectedResult.UNKNOWN_ERROR,
      ExpectedResult.WITNESS_MALLEATED,
      ExpectedResult.WITNESS_MALLEATED_P2SH

    )

    (checkedExpectedResults ++ notCheckedExpectedResults) should contain theSameElementsAs ExpectedResult.all

    val filteredScriptTests = scriptTests.filter { test =>
      checkedExpectedResults.contains(test.expectedResult)
    }

    filteredScriptTests.zipWithIndex.foreach(Function.tupled(run))
  }

  private def toScriptFlags(scriptFlagsString: String): Seq[ScriptFlag] = {
    scriptFlagsString.split(",").map(_.trim).flatMap(ScriptFlag.fromString)
  }

  private def stripDoubleQuotes(config: ConfigValue): String = {
    val raw = config.render(ConfigRenderOptions.concise())
    stripDoubleQuotes(raw)
  }

  private def stripDoubleQuotes(raw: String): String = {
    (raw.length >= 2 && raw.head == '\"' && raw.last == '\"')
      .option(raw.drop(1).dropRight(1))
      .getOrElse(raw)
  }
}

object niux {
  import io.github.yzernik.bitcoinscodec.messages.TxWitness
  import io.github.yzernik.bitcoinscodec.structures.{Hash, OutPoint, TxIn, TxOutWitness}
  import me.hongchao.bitcoin4s.script.ConstantOp._
  import me.hongchao.bitcoin4s.script.ScriptFlag._
  import me.hongchao.bitcoin4s.script.SigVersion.SIGVERSION_BASE
  import me.hongchao.bitcoin4s.script.ScriptExecutionStage.ExecutingScriptSig
  import scodec.bits._
  import cats.implicits._

  val transaction = TxWitness(
    version = 1,
    tx_in = List(
      TxIn(
        previous_output = OutPoint(Hash(hex"7ca98806a4b4ab8d2952d3d65ccb450b411def420b3f8f0140bf11d8991ac5ab"), 0),
        sig_script = ByteVector.empty,
        sequence = -1
      )
    )
    ,tx_out = List(
      TxOutWitness(
        value = 1,
        pk_script = ByteVector.empty
      )
    ),
    lock_time = 0
  )

  val initialState = InterpreterState(
    scriptPubKey = Seq(
      OP_0, OP_PUSHDATA(32),
      ScriptConstant(List(24, 99, 20, 60, 20, -59, 22, 104, 4, -67, 25, 32, 51, 86, -38, 19, 108, -104, 86, 120, -51, 77, 39, -95, -72, -58, 50, -106, 4, -112, 50, 98))
    ),
    scriptSig = Seq.empty[ScriptElement],
    currentScript = Seq.empty[ScriptElement],
    scriptP2sh = None,
    scriptWitness = None,
    scriptWitnessStack = Some(
      List(
        ScriptConstant(Seq(33, 2, 121, -66, 102, 126, -7, -36, -69, -84, 85, -96, 98, -107, -50, -121, 11, 7, 2, -101, -4, -37, 45, -50, 40, -39, 89, -14, -127, 91, 22, -8, 23, -104, -84)),
        ScriptConstant(Seq(48, 68, 2, 32, 66, 86, 20, 111, -49, -114, 115, -80, -3, -127, 127, -6, 42, 78, 64, -113, -16, 65, -113, -7, -121, -35, 8, -92, -12, -123, -74, 37, 70, -10, -60, 60, 2, 32, 63, 60, -116, 62, 47, -21, -64, 81, -31, 34, 40, 103, -11, -7, -48, -22, -16, 57, -42, 121, 41, 17, -63, 9, 64, -86, 60, -57, 65, 35, 55, -114, 1))
      )
    ),
    stack = List(),
    altStack = List(),
    flags = Seq(SCRIPT_VERIFY_P2SH, SCRIPT_VERIFY_WITNESS, SCRIPT_VERIFY_WITNESS_PUBKEYTYPE),
    opCount = 0,
    transaction = transaction,
    inputIndex = 0,
    amount = 1,
    sigVersion = SIGVERSION_BASE,
    scriptExecutionStage = ExecutingScriptSig
  )


  val result = Interpreter.interpret().run(initialState)

}
