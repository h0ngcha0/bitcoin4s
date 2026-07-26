package it.softfork.bitcoin4s.script

import scala.collection.immutable.ArraySeq
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.control.Exception.allCatch

import com.typesafe.config._
import org.spongycastle.util.encoders.Hex

import it.softfork.bitcoin4s.Spec
import it.softfork.bitcoin4s.crypto.Secp256k1
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
      .filter(hasSupportedFlags)

    lazy val scriptTests = rawScriptTests.collect { case elements @ (head :: tail) =>
      if (head.isInstanceOf[ConfigList]) {
        val witnessElement = head.toList().map(stripDoubleQuotes)
        val amount = (BigDecimal(witnessElement.last) * 100000000).toLong
        val (expandedWitness, taprootOutput) =
          expandTaprootWitness(witnessElement.dropRight(1))
        val stringTail = tail.map(stripDoubleQuotes)
        val comments = (stringTail.length == 5).option(stringTail.last).getOrElse("")
        val witnesses = expandedWitness.reverse
          .map(ScriptConstant.apply)
        val List(scriptSigString, rawScriptPubKeyString, scriptFlagsString, expectedResultString) =
          stringTail.take(4)
        val scriptPubKeyString = taprootOutput
          .map(output => s"0x51 0x20 0x${output.toHex}")
          .getOrElse(rawScriptPubKeyString)
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
        val List(scriptSigString, scriptPubKeyString, scriptFlagsString, expectedResultString) =
          stringElements.take(4)
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

  private def hasSupportedFlags(elements: List[ConfigValue]): Boolean = {
    val fields = if (elements.head.isInstanceOf[ConfigList]) elements.tail else elements
    val flags = stripDoubleQuotes(fields(2)).split(",").map(_.trim).filter(_.nonEmpty)
    flags.forall(ScriptFlag.fromString(_).isDefined)
  }

  private def toScriptFlags(scriptFlagsString: String): Seq[ScriptFlag] = {
    val result = scriptFlagsString
      .split(",")
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(ScriptFlag.fromString(_).value)
    ArraySeq.unsafeWrapArray(result)
  }

  private def expandTaprootWitness(
    witness: List[String]
  ): (List[Array[Byte]], Option[Seq[Byte]]) = {
    val internalPublicKey = Secp256k1.params.getG.getEncoded(true).tail.toSeq

    witness.foldLeft((List.empty[Array[Byte]], Option.empty[Seq[Byte]])) {
      case ((elements, output), raw) if raw.startsWith("#SCRIPT#") =>
        val script = Parser.parse(raw.stripPrefix("#SCRIPT#").trim).flatMap(_.bytes).toArray
        (elements :+ script, output)

      case ((elements, _), "#CONTROLBLOCK#") =>
        val script = elements.last
        val leafHash = Taproot.tapLeafHash(0xc0, script.toIndexedSeq)
        val (taprootOutput, odd) =
          Taproot.outputKey(internalPublicKey, leafHash.toIndexedSeq).value
        val control = Array((0xc0 | odd.option(1).getOrElse(0)).toByte) ++
          internalPublicKey
        (elements :+ control, Some(taprootOutput))

      case ((elements, output), raw) =>
        val decoded = allCatch.opt(Hex.decode(raw)).value
        (elements :+ decoded, output)
    }
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
