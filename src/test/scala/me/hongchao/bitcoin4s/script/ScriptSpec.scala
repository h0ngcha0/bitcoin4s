package me.hongchao.bitcoin4s.script

import com.typesafe.config._

import scala.collection.JavaConverters._
import me.hongchao.bitcoin4s.Spec
import me.hongchao.bitcoin4s.Utils._

import scala.io.Source

class ScriptSpec extends Spec {
  case class TestCase(
    scriptSig: Seq[ScriptElement],
    scriptPubKey: Seq[ScriptElement],
    scriptFlags: Seq[ScriptFlag],
    expectedResult: ExpectedResult,
    comments: String,
    witness: Option[(List[String], BigInt)]
  )

  sealed trait ExpectedResult extends Product {
    val name = productPrefix
    override def toString: String = name
  }

  object ExpectedResult {
    case object OK extends ExpectedResult
    case object EVAL_FALSE extends ExpectedResult
    case object BAD_OPCODE extends ExpectedResult
    case object UNBALANCED_CONDITIONAL extends ExpectedResult
    case object OP_RETURN extends ExpectedResult
    case object VERIFY extends ExpectedResult
    case object INVALID_ALTSTACK_OPERATION extends ExpectedResult
    case object INVALID_STACK_OPERATION extends ExpectedResult
    case object EQUALVERIFY extends ExpectedResult
    case object DISABLED_OPCODE extends ExpectedResult
    case object UNKNOWN_ERROR extends ExpectedResult
    case object DISCOURAGE_UPGRADABLE_NOPS extends ExpectedResult
    case object PUSH_SIZE extends ExpectedResult
    case object OP_COUNT extends ExpectedResult
    case object STACK_SIZE extends ExpectedResult
    case object SCRIPT_SIZE extends ExpectedResult
    case object PUBKEY_COUNT extends ExpectedResult
    case object SIG_COUNT extends ExpectedResult
    case object SIG_PUSHONLY extends ExpectedResult
    case object MINIMALDATA extends ExpectedResult
    case object PUBKEYTYPE extends ExpectedResult
    case object SIG_DER extends ExpectedResult
    case object WITNESS_PROGRAM_MISMATCH extends ExpectedResult
    case object NULLFAIL extends ExpectedResult
    case object SIG_HIGH_S extends ExpectedResult
    case object SIG_HASHTYPE extends ExpectedResult
    case object SIG_NULLDUMMY extends ExpectedResult
    case object CLEANSTACK extends ExpectedResult
    case object DISCOURAGE_UPGRADABLE_WITNESS_PROGRAM extends ExpectedResult
    case object WITNESS_PROGRAM_WRONG_LENGTH extends ExpectedResult
    case object WITNESS_PROGRAM_WITNESS_EMPTY extends ExpectedResult
    case object WITNESS_MALLEATED extends ExpectedResult
    case object WITNESS_MALLEATED_P2SH extends ExpectedResult
    case object WITNESS_UNEXPECTED extends ExpectedResult
    case object WITNESS_PUBKEYTYPE extends ExpectedResult
    case object NEGATIVE_LOCKTIME extends ExpectedResult
    case object UNSATISFIED_LOCKTIME extends ExpectedResult
    case object MINIMALIF extends ExpectedResult

    val all = Seq(
      OK, EVAL_FALSE, BAD_OPCODE, UNBALANCED_CONDITIONAL, OP_RETURN, VERIFY,
      INVALID_ALTSTACK_OPERATION, INVALID_STACK_OPERATION, EQUALVERIFY,
      DISABLED_OPCODE, UNKNOWN_ERROR, DISCOURAGE_UPGRADABLE_NOPS, PUSH_SIZE,
      OP_COUNT, STACK_SIZE, SCRIPT_SIZE, PUBKEY_COUNT, SIG_COUNT, SIG_PUSHONLY,
      MINIMALDATA, PUBKEYTYPE, SIG_DER, WITNESS_PROGRAM_MISMATCH, NULLFAIL,
      SIG_HIGH_S, SIG_HASHTYPE, SIG_NULLDUMMY, CLEANSTACK,
      DISCOURAGE_UPGRADABLE_WITNESS_PROGRAM, WITNESS_PROGRAM_WRONG_LENGTH,
      WITNESS_PROGRAM_WITNESS_EMPTY, WITNESS_MALLEATED, WITNESS_MALLEATED_P2SH,
      WITNESS_UNEXPECTED, WITNESS_PUBKEYTYPE, NEGATIVE_LOCKTIME, UNSATISFIED_LOCKTIME,
      MINIMALIF
    )

    def fromString(str: String) = all.find(_.name == str)
  }

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

  "Script" should "test cases" in {
    val scriptTestsConfig: List[List[ConfigValue]] = testConfig
      .getList("bitcoin4s.script_tests")
      .toList
      .map(_.toList)

    val rawScriptTests = scriptTestsConfig
      .filter(_.length > 3)

    val scriptTests = rawScriptTests.collect {
      case elements @ (head :: tail)  =>
        if (head.isInstanceOf[ConfigList]) {
          val witnessElement = head.toList.map(_.render)
          val amount = (BigDecimal(witnessElement.last) * 10000000).toBigInt
          val witnesses = witnessElement.reverse.tail
          val stringTail = tail.map(stripDoubleQuotes)
          val List(scriptSig, scriptPubKey, scriptFlags, expectedResultString) = stringTail.take(4)
          val expectedResult = ExpectedResult.fromString(expectedResultString).value
          val comments = (stringTail.length == 5).option(stringTail.last).getOrElse("")

          TestCase(
            scriptSig = Parser.parse(scriptSig),
            scriptPubKey = Parser.parse(scriptPubKey),
            scriptFlags = scriptFlags.split(",").map(_.trim).flatMap(ScriptFlag.fromString),
            expectedResult = expectedResult,
            comments = comments,
            witness = Some((witnesses, amount))
          )
        } else {
          val stringElements = elements.map(stripDoubleQuotes)
          val List(scriptSig, scriptPubKey, scriptFlags, expectedResultString) = stringElements.take(4)
          val expectedResult = ExpectedResult.fromString(expectedResultString).value
          val comments = (stringElements.length == 5).option(stringElements.last).getOrElse("")

          val parsedScriptSig = Parser.parse(scriptSig)
          val parsedScriptPubKey = Parser.parse(scriptPubKey)

          TestCase(
            scriptSig = parsedScriptSig,
            scriptPubKey = parsedScriptPubKey,
            scriptFlags = scriptFlags.split(",").map(_.trim).flatMap(ScriptFlag.fromString),
            expectedResult = expectedResult,
            comments = comments,
            witness = None
          )
        }
    }
  }

  private def stripDoubleQuotes(config: ConfigValue): String = {
    val raw = config.render(ConfigRenderOptions.concise())
    (raw.length >= 2 && raw.head == '\"' && raw.last == '\"')
      .option(raw.drop(1).dropRight(1))
      .getOrElse(raw)
  }
}
