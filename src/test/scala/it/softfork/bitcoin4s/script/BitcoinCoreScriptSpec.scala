package it.softfork.bitcoin4s.script

import scala.collection.immutable.ArraySeq
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.control.Exception.allCatch

import com.typesafe.config._
import org.spongycastle.util.encoders.Hex

import it.softfork.bitcoin4s.Spec
import it.softfork.bitcoin4s.utils._

// Run bitcoin core script_test.json test cases
class BitcoinCoreScriptSpec extends Spec with BitcoinCoreScriptTestRunner {

  implicit class RichConfigValue(configValue: ConfigValue) {

    def toList(): List[ConfigValue] = {
      configValue.asInstanceOf[ConfigList].iterator().asScala.toList
    }
  }

  val testConfigString = Source.fromURI(getClass.getResource("/script_test.json").toURI).mkString
  val testConfig: Config = ConfigFactory.parseString(s"""
       |bitcoin4s {
       |  script_tests = $testConfigString
       |}
     """.stripMargin)

  "Interpreter" should "pass script_test.json in bitcoin reference client code base" in {
    val scriptTestsConfig: List[List[ConfigValue]] = testConfig
      .getList("bitcoin4s.script_tests")
      .toList()
      .map(_.toList())

    val rawScriptTests = scriptTestsConfig
      .filter(_.length > 3)

    lazy val scriptTests = rawScriptTests.collect {
      case elements @ (head :: tail) =>
        if (head.isInstanceOf[ConfigList]) {
          val witnessElement = head.toList().map(_.render)
          val amount = (BigDecimal(witnessElement.last) * 100000000).toLong
          val stringTail = tail.map(stripDoubleQuotes)
          val comments = (stringTail.length == 5).option(stringTail.last).getOrElse("")
          val witnesses = witnessElement.reverse.tail
            .flatMap { rawWitness =>
              allCatch.opt(Hex.decode(stripDoubleQuotes(rawWitness)))
            }
            .map(ScriptConstant.apply)
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

    scriptTests.zipWithIndex.foreach(Function.tupled(run))
  }

  private def toScriptFlags(scriptFlagsString: String): Seq[ScriptFlag] = {
    val result = scriptFlagsString.split(",").map(_.trim).flatMap(ScriptFlag.fromString _)
    ArraySeq.unsafeWrapArray(result)
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
