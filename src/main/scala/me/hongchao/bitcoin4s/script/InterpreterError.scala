package me.hongchao.bitcoin4s.script

sealed trait InterpreterError extends RuntimeException with Product {
  def opCode: ScriptOpCode
  val state: InterpreterState
  def description: String = getClass.getSimpleName

  super.initCause(new Throwable(s"$description\nopCode: $opCode\nstate: $state"))
}

object InterpreterError {
  case class BadOpCode(opCode: ScriptOpCode, state: InterpreterState, descriptionIn: String) extends InterpreterError {
    override def description = descriptionIn
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

  case class InvalidStackOperation(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class InvalidAltStackOperation(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class RequireCleanStack(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "Stack is not clean when SCRIPT_VERIFY_CLEANSTACK flag is set"
  }

  case class NotMinimalEncoding(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "MINIMALENCODING flags is set but it's not minimal encoded"
  }

  case class ExceedMaxOpsCount(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class FoundOpReturn(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class ExceedMaxPushSize(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class ExceedMaxStackSize(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class ExceedMaxScriptSize(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class WrongPubKeyCount(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class WrongSignaturesCount(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class ScriptSigPushOnly(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "Script sig should only contain pusheOnly ops"
  }

  case class NotAllOperantsAreConstant(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class OperantMustBeScriptNum(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class OperantMustBeScriptConstant(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class OpcodeDisabled(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class DiscourageUpgradableNops(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "Found not executable reserved opcode"
  }

  case class DiscourageUpgradableWitnessProgram(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "Witness program version is too high"
  }

  case class InValidReservedOpcode(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "Found executable reserved opcode that invalidates the transaction"
  }

  case class PublicKeyWrongEncoding(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class SignatureWrongEncoding(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class InvalidSigHashType(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class MultiSigNullDummy(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "Multisig dummy element is not null"
  }

  case class VerificationFailed(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "Verification on top of the stack failed"
  }

  case class SignatureVerificationNullFail(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "If NULLFAIL flag is on and signature verification fails, signature has to be empty for script to continue"
  }

  case class CLTVFailed(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "CheckLockTimeVerify failed"
  }

  case class CSVFailed(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "CheckSequenceVerify failed"
  }

  case class UnbalancedConditional(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class NoSerializedP2SHScriptFound(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class WitnessProgramMismatch(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class WitnessProgramUnexpected(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class WitnessProgramWrongLength(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class WitnessProgramWitnessEmpty(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class SignatureHighS(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "Secp256k1 signature has higher S value than half curve"
  }

  case class MinimalIf(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {
    override def description = "Argument of OP_IF/OP_NOTIF is not empty bytes or 0x01"
  }

  case class WitnessPubkeyUncompressed(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class WitnessMalleated(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class WitnessMalleatedP2SH(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}

  case class GeneralError(opCode: ScriptOpCode, state: InterpreterState) extends InterpreterError {}
}
