package it.softfork.bitcoin4s.script

import it.softfork.bitcoin4s.Spec
import it.softfork.bitcoin4s.script.SigVersion.SIGVERSION_WITNESS_V0
import it.softfork.bitcoin4s.transaction.Tx
import cats.implicits._
import it.softfork.bitcoin4s.script.BitwiseLogicOp.OP_EQUAL
import it.softfork.bitcoin4s.script.ConstantOp.OP_FALSE
import it.softfork.bitcoin4s.script.Interpreter.InterpreterErrorHandler
import it.softfork.bitcoin4s.script.ScriptExecutionStage.ExecutingScriptPubKey
import it.softfork.bitcoin4s.script.StackOp.OP_DEPTH

class StepExecutionSpec extends Spec {

  implicit class InterpreterResultExtractor(
    interpreterResult: InterpreterErrorHandler[(InterpreterState, Option[Boolean])]
  ) {

    def currentScript = {
      getInterpreterState.currentScript
    }

    def stack = {
      getInterpreterState.stack
    }

    def altStack = {
      getInterpreterState.altStack
    }

    def executionStage = {
      getInterpreterState.scriptExecutionStage
    }

    def getInterpreterState: InterpreterState = {
      interpreterResult.toOption.value._1
    }

    def result: Option[Boolean] = {
      interpreterResult.toOption.value._2
    }
  }

  "Interpreter" should "execute only the number steps specified" in {
    val scriptPubKey = Parser.parse("DEPTH 0 EQUAL")
    val scriptSig = Parser.parse("")

    val firstStep = execScript(scriptPubKey, scriptSig, maybeSteps = Some(1))
    firstStep.currentScript shouldEqual List(OP_DEPTH, OP_FALSE, OP_EQUAL)
    firstStep.stack shouldBe empty
    firstStep.altStack shouldBe empty
    firstStep.executionStage shouldBe ExecutingScriptPubKey
    firstStep.result shouldBe None

    val secondStep = execScript(scriptPubKey, scriptSig, maybeSteps = Some(2))
    secondStep.currentScript shouldEqual List(OP_FALSE, OP_EQUAL)
    secondStep.stack shouldBe Seq(ScriptNum(0))
    secondStep.altStack shouldBe empty
    secondStep.executionStage shouldBe ExecutingScriptPubKey
    secondStep.result shouldBe None

    val thirdStep = execScript(scriptPubKey, scriptSig, maybeSteps = Some(3))
    thirdStep.currentScript shouldEqual List(OP_EQUAL)
    thirdStep.stack shouldBe Seq(ScriptConstant(Array.empty), ScriptNum(0))
    thirdStep.altStack shouldBe empty
    thirdStep.executionStage shouldBe ExecutingScriptPubKey
    thirdStep.result shouldBe None

    val fourthStep = execScript(scriptPubKey, scriptSig, maybeSteps = Some(4))
    fourthStep.currentScript shouldEqual List()
    fourthStep.stack shouldBe Seq(ScriptNum(1))
    fourthStep.altStack shouldBe empty
    fourthStep.executionStage shouldBe ExecutingScriptPubKey
    fourthStep.result shouldBe None

    val fifthStep = execScript(scriptPubKey, scriptSig, maybeSteps = Some(5))
    fifthStep.currentScript shouldEqual List()
    fifthStep.stack shouldBe Seq(ScriptNum(1))
    fifthStep.altStack shouldBe empty
    fifthStep.executionStage shouldBe ExecutingScriptPubKey
    fifthStep.result shouldBe Some(true)

    val withoutStepping = execScript(scriptPubKey, scriptSig, maybeSteps = None)
    fifthStep shouldEqual withoutStepping
  }

  private def execScript(
    scriptPubKey: Seq[ScriptElement],
    scriptSig: Seq[ScriptElement],
    maybeSteps: Option[Int]
  ): InterpreterErrorHandler[(InterpreterState, Option[Boolean])] = {
    // A fake transaction that is not used since no crypto ops are involved.
    val emptyTx = Tx(
      version = 1,
      flag = false,
      tx_in = List.empty,
      tx_out = List.empty,
      tx_witness = List.empty,
      lock_time = 1000000
    )

    val initialState = InterpreterState(
      scriptPubKey = scriptPubKey,
      scriptSig = scriptSig,
      scriptWitnessStack = None,
      flags = Seq.empty,
      transaction = emptyTx,
      inputIndex = 0,
      amount = 10,
      sigVersion = SIGVERSION_WITNESS_V0
    )

    Interpreter.create(verbose = false, maybeSteps).run(initialState)
  }
}
