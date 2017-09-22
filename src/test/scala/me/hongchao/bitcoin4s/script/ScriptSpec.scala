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
    scriptFlags: String,
    expectedResult: String,
    comments: String,
    witness: Option[(List[String], BigInt)]
  )

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

    val rawScriptTests = scriptTestsConfig.filter(_.length > 3).take(1)
    println(s"rawScriptTests: $rawScriptTests")

    val scriptTests = rawScriptTests.collect {
      case elements @ (head :: tail)  =>
        if (head.isInstanceOf[ConfigList]) {
          val witnessElement = head.toList.map(_.render)
          val amount = (BigDecimal(witnessElement.last) * 10000000).toBigInt
          val witnesses = witnessElement.reverse.tail
          val stringTail = tail.map(_.render)
          val List(scriptSig, scriptPubKey, scriptFlags, expectedResult) = stringTail.take(4)
          val comments = (stringTail.length == 5).option(stringTail.last).getOrElse("")

          TestCase(
            scriptSig = Parser.parse(scriptSig),
            scriptPubKey = Parser.parse(scriptPubKey),
            scriptFlags = scriptFlags,
            expectedResult = expectedResult,
            comments = comments,
            witness = Some((witnesses, amount))
          )
        } else {
          val stringElements = elements.map(_.render(ConfigRenderOptions.concise())).map(_.drop(1).dropRight(1))
          println(s"stringElements: $stringElements")
          val List(scriptSig, scriptPubKey, scriptFlags, expectedResult) = stringElements.take(4)
          val comments = (stringElements.length == 5).option(stringElements.last).getOrElse("")

          println(s"scriptSig: $scriptSig, ${scriptSig.length}")
          val parsedScriptSig = Parser.parse(scriptSig)
          println(s"parsedScriptSig: $parsedScriptSig")

          val parsedScriptPubKey = Parser.parse(scriptPubKey)
          println(s"parsedScriptPubKey: $parsedScriptPubKey")

          TestCase(
            scriptSig = parsedScriptSig,
            scriptPubKey = parsedScriptPubKey,
            scriptFlags = scriptFlags,
            expectedResult = expectedResult,
            comments = comments,
            witness = None
          )
        }
    }

    println(s"scriptTests: $scriptTests")
  }
}
