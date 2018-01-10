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
      ExpectedResult.SIG_NULLDUMMY
    )

    val notCheckedExpectedResults = Seq(
      ExpectedResult.UNKNOWN_ERROR,
      ExpectedResult.WITNESS_PROGRAM_MISMATCH,
      ExpectedResult.SIG_HIGH_S,
      ExpectedResult.SIG_HASHTYPE,
      ExpectedResult.DISCOURAGE_UPGRADABLE_WITNESS_PROGRAM,
      ExpectedResult.WITNESS_PROGRAM_WRONG_LENGTH,
      ExpectedResult.WITNESS_PROGRAM_WITNESS_EMPTY,
      ExpectedResult.WITNESS_MALLEATED,
      ExpectedResult.WITNESS_MALLEATED_P2SH,
      ExpectedResult.WITNESS_UNEXPECTED,
      ExpectedResult.WITNESS_PUBKEYTYPE,
      ExpectedResult.UNSATISFIED_LOCKTIME,
      ExpectedResult.MINIMALIF
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
