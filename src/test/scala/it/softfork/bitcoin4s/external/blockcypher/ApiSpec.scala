package it.softfork.bitcoin4s.external.blockcypher

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import it.softfork.bitcoin4s.Spec
import it.softfork.bitcoin4s.crypto.Hash
import it.softfork.bitcoin4s.script.SigVersion.SIGVERSION_WITNESS_V0
import it.softfork.bitcoin4s.script._
import org.spongycastle.util.encoders.Hex

import scala.collection.immutable.ArraySeq
import scala.io.Source
import scala.util.control.Exception.allCatch

class ApiSpec extends Spec with StrictLogging {

  "Interpreter" should "be able to interpret raw transactions from BlockCypher API" in {
    Seq("p2sh-example1", "p2pkh-example1", "p2pkh-example2", "multisig-example1").foreach { example =>
      val rawCreditingTransaction = rawJsonFromResource(s"/blockcypher/$example/crediting_tx.json")
      val rawSpendingTransaction = rawJsonFromResource(s"/blockcypher/$example/spending_tx.json")

      withClue(example) {
        runExample(rawCreditingTransaction, rawSpendingTransaction)
      }
    }
  }

  private def runExample(rawCreditingTransaction: String, rawSpendingTransaction: String) = {
    val creditingTransaction = Api.parseTransaction(rawCreditingTransaction)
    val spendingTransaction = Api.parseTransaction(rawSpendingTransaction)

    // Only look at the first input
    //val firstScriptPutKey = spendingTx.tx_in(0).sig_script
    val spendingTx = spendingTransaction.toTx
    val txIn = spendingTransaction.inputs(0)
    val txOut = creditingTransaction.outputs(txIn.output_index)
    val scriptSig = txIn.script.map(parseHexString _).getOrElse(Seq.empty[ScriptElement])
    val scriptPubKey = parseHexString(txOut.script)
    val witnessesStack = txIn.witness.map { rawWitnesses =>
      rawWitnesses.reverse.flatMap { rawWitness =>
        allCatch.opt(Hex.decode(rawWitness)).map(ScriptConstant.apply)
      }
    }

    val amount = txOut.value
    val flags = toScriptFlags("P2SH,WITNESS")

    val initialState = InterpreterState(
      scriptPubKey = scriptPubKey,
      scriptSig = scriptSig,
      scriptWitnessStack = witnessesStack,
      flags = flags,
      transaction = spendingTx,
      inputIndex = 0,
      amount = amount,
      sigVersion = SIGVERSION_WITNESS_V0
    )

    val interpreterOutcome = Interpreter.create(verbose = false).run(initialState)

    interpreterOutcome match {
      case Right((finalState @ _, interpretedResult)) =>
        interpretedResult.value shouldBe true
        logger.info(s"interpretedResult: $interpretedResult")

      case Left(e) =>
        fail("Interpreter failed", e)
    }
  }

  private def parseHexString(hex: String): Seq[ScriptElement] = {
    Parser.parse(ArraySeq.unsafeWrapArray(Hash.fromHex(hex)))
  }

  private def toScriptFlags(scriptFlagsString: String): Seq[ScriptFlag] = {
    val result = scriptFlagsString.split(",").map(_.trim).flatMap(ScriptFlag.fromString _)
    ArraySeq.unsafeWrapArray(result)
  }

  private def rawJsonFromResource(resourcePath: String): String = {
    Source.fromURI(getClass.getResource(resourcePath).toURI).mkString
  }
}
