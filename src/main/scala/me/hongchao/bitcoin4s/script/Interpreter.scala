package me.hongchao.bitcoin4s.script

import io.github.yzernik.bitcoinscodec.messages.Tx
import io.github.yzernik.bitcoinscodec.structures.TxIn
import simulacrum._

case class InterpreterContext(
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
  def replaceStackTopElement(scriptElement: ScriptElement): InterpreterContext = {
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
}

@typeclass trait Interpreter[A <: ScriptOpCode] {
  // def interpret[A](opCode: A): State[InterpreterContext, Either[String, Boolean]]
  def interpret(opCode: A, context: InterpreterContext): InterpreterContext
}