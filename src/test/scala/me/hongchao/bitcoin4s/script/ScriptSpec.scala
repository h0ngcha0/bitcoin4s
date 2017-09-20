package me.hongchao.bitcoin4s.script

import com.typesafe.config.ConfigList

import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigFactory, ConfigValue}
import me.hongchao.bitcoin4s.Spec
import me.hongchao.bitcoin4s.Utils._

import scala.io.Source

class ScriptSpec extends Spec {
  case class TestCase(
    scriptSig: String,
    scriptPubKey: String,
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

    val scriptTests = scriptTestsConfig.filter(_.length > 3).collect {
      case elements @ (head :: tail)  =>
        if (head.isInstanceOf[ConfigList]) {
          val witnessElement = head.toList.map(_.render)
          val amount = (BigDecimal(witnessElement.last) * 10000000).toBigInt
          val witnesses = witnessElement.reverse.tail
          val stringTail = tail.map(_.render)

          val List(scriptSig, scriptPubKey, scriptFlags, expectedResult) = stringTail.take(4)
          val comments = (stringTail.length == 5).option(stringTail.last).getOrElse("")
          TestCase(scriptSig, scriptPubKey, scriptFlags, expectedResult, comments, Some((witnesses, amount)))
        } else {
          val stringElements = elements.map(_.render)

          val List(scriptSig, scriptPubKey, scriptFlags, expectedResult) = stringElements.take(4)
          val comments = (stringElements.length == 5).option(stringElements.last).getOrElse("")
          TestCase(scriptSig, scriptPubKey, scriptFlags, expectedResult, comments, None)
        }
    }

    println(scriptTests)
  }


}
