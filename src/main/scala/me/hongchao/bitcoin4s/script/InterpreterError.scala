package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.script.OpCodes.OP_UNKNOWN

sealed trait InterpreterError extends RuntimeException with Product {
  def opCode: ScriptOpCode
  val state: InterpreterState
  val description: String

  super.initCause(new Throwable(s"$description\nopCode: $opCode\nstate: $state"))
}

object InterpreterError {
  case class BadOpCode(opCode: ScriptOpCode, state: InterpreterState, description: String) extends InterpreterError {
  }

  object NotEnoughElementsInStack {
    def apply(opCode: ScriptOpCode, state: InterpreterState) = {
      BadOpCode(opCode: ScriptOpCode, state: InterpreterState, "Not enough elements in the stack")
    }
  }

  object NotExecutableReservedOpcode{
    def apply(opCode: ScriptOpCode, state: InterpreterState) = {
      BadOpCode(opCode: ScriptOpCode, state: InterpreterState, "Found not executable reserved opcode")
    }
  }

  case class InvalidStackOperation(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Invalid stack operation"
  }

  case class InvalidAltStackOperation(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Invalid alt stack operation"
  }

  case class RequireCleanStack(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Stack is not clean when SCRIPT_VERIFY_CLEANSTACK flag is set"
  }

  case class NotMinimalEncoding(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "MINIMALENCODING flags is set but it's not minimal encoded"
  }

  case class ExceedMaxOpCount(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "More than max op count"
  }

  case class FoundOpReturn(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description: String = "Found OP_RETURN"
  }

  case class ExceedMaxPushSize(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Exceed the maximum push size"
  }

  case class ExceedMaxStackSize(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Exceed the maximum stack size"
  }

  case class ExceedMaxScriptSize(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Exceed the maximum script size"
  }

  case class WrongPubKeyCount(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Wrong pubkey count"
  }

  case class WrongSignaturesCount(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Wrong signatures count"
  }

  case class ScriptSigPushOnly(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Script sig should only contain pusheOnly ops"
  }

  case class NotAllOperantsAreConstant(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Not all operants are constant"
  }

  case class OperantMustBeScriptNum(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Operant must be ScriptNum"
  }

  case class OperantMustBeScriptConstant(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Operant must be ScriptConstant"
  }

  case class OpcodeDisabled(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Opcode is disabled"
  }

  case class DiscourageUpgradableNops(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Found not executable reserved opcode"
  }

  case class InValidReservedOpcode(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Found executable reserved opcode that invalidates the transaction"
  }

  case class PublicKeyWrongEncoding(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Public key encoding wrong"
  }

  case class SignatureWrongEncoding(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Signature encoding wrong"
  }

  case class InvalidSigHashType(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "SigHash type invalid"
  }

  case class MultiSigNullDummy(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Multisig dummy element is not null"
  }

  case class VerificationFailed(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "Verification on top of the stack failed"
  }

  case class SignatureVerificationNullFail(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "If NULLFAIL flag is on and signature verification fails, signature has to be empty for script to continue"
  }

  case class CLTVFailed(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "CheckLockTimeVerify failed"
  }

  case class CSVFailed(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description = "CheckSequenceVerify failed"
  }

  case class UnbalancedConditional(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description: String = "Unbalanced conditional"
  }

  case class NoSerializedScriptFound(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description: String = "No serialized script found for p2sh"
  }

  case class NotImplemented(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description: String = "Not implemented"
  }

  // FIXME: Error should all be specific
  case class GeneralError(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    val description: String = "General error"
  }
}
