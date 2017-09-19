package me.hongchao.bitcoin4s.script

import cats.data.State
import io.github.yzernik.bitcoinscodec.messages.Tx
import io.github.yzernik.bitcoinscodec.structures.TxIn
import me.hongchao.bitcoin4s.script.Interpreter.InterpreterContext
import simulacrum._

case class InterpreterState(
  script: Seq[ScriptElement],
  stack: Seq[ScriptElement],
  altStack: Seq[ScriptElement],
  flags: Seq[ScriptFlag],
  opCount: Int,
  transaction: Tx,
  inputIndex: Int,
  scriptPubKey: Seq[ScriptElement],
  sigVersion: SigVersion
) {
  // Execute one OpCode, which takes the stack top element and and produce a new
  // value. The new value it put on top of the stack
  def replaceStackTopElement(scriptElement: ScriptElement): InterpreterState = {
    copy(
      script = script.tail,
      stack = scriptElement +: stack.tail,
      opCount = opCount + 1
    )
  }

  def transactionInput: TxIn = {
    require(inputIndex >= 0 && inputIndex < transaction.tx_in.length, "Transation input index must be within range.")
    transaction.tx_in(inputIndex)
  }

  def scriptSignature: Seq[Byte] = transactionInput.sig_script.toSeq
}


sealed trait InterpreterError extends RuntimeException {
  val opCode: ScriptOpCode
  val stack: Seq[ScriptElement]
  val description: String

  super.initCause(new Throwable(s"$description\nopCode: $opCode\nstack: $stack"))
}

object InterpreterError {
  case class NotEnoughElementsInStack(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Not enough elements in the stack"
  }

  case class NotEnoughElementsInAltStack(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Not enough elements in the alternative stack"
  }

  case class NotAllOperantsAreConstant(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Not all operants are constant"
  }

  case class OperantMustBeScriptNum(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Operant must be ScriptNum"
  }

  case class OperantMustBeScriptConstant(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Operant must be ScriptConstant"
  }

  case class OpcodeDisabled(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Opcode is disabled"
  }

  case class NotExecutableReservedOpcode(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Found not executable reserved opcode"
  }

  case class InValidReservedOpcode(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Found executable reserved opcode that invalidates the transaction"
  }

  case class VerificationFailed(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Verification on top of the stack failed"
  }

  case class NotImplemented(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description = "Not implemented"
  }

  case class UnbalancedCondition(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description: String = "Unbalanced condition"
  }

  case class UnexpectedOpCode(opCode: ScriptOpCode, stack: Seq[ScriptElement]) extends InterpreterError {
    val description: String = "Unexpected op code encountered"
  }
}

object Interpreter {
  type InterpreterResult = Either[InterpreterError, Option[Boolean]]
  type InterpreterContext = State[InterpreterState, InterpreterResult]

  def continue: Any => InterpreterContext  = _ => State.pure(Right(None))
  def abort(error: InterpreterError): InterpreterContext = State.pure(Left(error))
}

@typeclass trait Interpretable[A <: ScriptOpCode] {
  def interpret(opCodes: A): InterpreterContext
}
