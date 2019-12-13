package it.softfork.bitcoin4s.script

import it.softfork.bitcoin4s.transaction.Tx
import it.softfork.bitcoin4s.transaction.TxIn
import it.softfork.bitcoin4s.script.OpCodes.OP_UNKNOWN
import it.softfork.bitcoin4s.script.ConstantOp._
import it.softfork.bitcoin4s.script.CryptoOp.{OP_CHECKSIG, OP_HASH160}
import it.softfork.bitcoin4s.script.InterpreterError._
import it.softfork.bitcoin4s.Utils._
import ScriptExecutionStage._
import it.softfork.bitcoin4s.crypto.Hash
import it.softfork.bitcoin4s.script.BitwiseLogicOp.OP_EQUALVERIFY
import it.softfork.bitcoin4s.script.StackOp.OP_DUP

import cats.FlatMap
import cats.data._
import cats.implicits._

sealed trait ScriptExecutionStage

object ScriptExecutionStage {
  case object ExecutingScriptSig extends ScriptExecutionStage
  case object ExecutingScriptPubKey extends ScriptExecutionStage
  case object ExecutingScriptP2SH extends ScriptExecutionStage
  case object ExecutingScriptWitness extends ScriptExecutionStage
}

object InterpreterState {

  def apply(
    scriptPubKey: Seq[ScriptElement],
    scriptSig: Seq[ScriptElement],
    scriptWitnessStack: Option[Seq[ScriptElement]],
    flags: Seq[ScriptFlag],
    transaction: Tx,
    inputIndex: Int,
    amount: Long,
    sigVersion: SigVersion
  ): InterpreterState = {
    InterpreterState(
      scriptPubKey = scriptPubKey,
      scriptSig = scriptSig,
      scriptWitnessStack = scriptWitnessStack,
      currentScript = scriptSig,
      transaction = transaction,
      inputIndex = inputIndex,
      flags = flags,
      sigVersion = SigVersion.SIGVERSION_BASE,
      amount = amount
    )
  }
}

case class InterpreterState(
  scriptPubKey: Seq[ScriptElement],
  scriptSig: Seq[ScriptElement],
  currentScript: Seq[ScriptElement],
  scriptP2sh: Option[Seq[ScriptElement]] = None,
  scriptWitness: Option[Seq[ScriptElement]] = None,
  scriptWitnessStack: Option[Seq[ScriptElement]] = None,
  stack: Seq[ScriptElement] = Seq.empty,
  altStack: Seq[ScriptElement] = Seq.empty,
  flags: Seq[ScriptFlag],
  opCount: Int = 0,
  transaction: Tx,
  inputIndex: Int,
  amount: Long,
  sigVersion: SigVersion,
  scriptExecutionStage: ScriptExecutionStage = ScriptExecutionStage.ExecutingScriptSig
) {

  def transactionInput: TxIn = {
    val txInLength = transaction.tx_in.length
    require(inputIndex >= 0 && inputIndex < txInLength, s"Transaction input ${inputIndex} index must be within range [0 - ${txInLength}].")
    transaction.tx_in(inputIndex)
  }

  def scriptSignature: Seq[Byte] = transactionInput.sig_script.value.toSeq

  object ScriptFlags {

    def requireMinimalEncoding(): Boolean = {
      flags.contains(ScriptFlag.SCRIPT_VERIFY_MINIMALDATA)
    }

    def p2sh(): Boolean = {
      flags.contains(ScriptFlag.SCRIPT_VERIFY_P2SH)
    }

    def requireCleanStack(): Boolean = {
      flags.contains(ScriptFlag.SCRIPT_VERIFY_CLEANSTACK)
    }

    def strictEncoding(): Boolean = {
      flags.contains(ScriptFlag.SCRIPT_VERIFY_STRICTENC)
    }

    def witness(): Boolean = {
      flags.contains(ScriptFlag.SCRIPT_VERIFY_WITNESS)
    }
  }
}

object Interpreter {
  type InterpreterErrorHandler[T] = Either[InterpreterError, T]
  type InterpreterContext[T] = StateT[InterpreterErrorHandler, InterpreterState, T]

  val MAX_OPCODES = 201
  val MAX_PUSH_SIZE = 520
  val MAX_STACK_SIZE = 1000
  val MAX_SCRIPT_SIZE = 10000 // bytes

  def tailRecM[A, B] = FlatMap[InterpreterContext].tailRecM[A, B] _

  import it.softfork.bitcoin4s.script.InterpretableOp.ops._

  def continue: InterpreterContext[Option[Boolean]] = StateT.pure(None)

  def evaluated(result: Boolean): InterpreterContext[Option[Boolean]] = {
    StateT.pure(Some(result))
  }

  def getState: InterpreterContext[InterpreterState] = {
    StateT.get[InterpreterErrorHandler, InterpreterState]
  }

  def setState(newState: InterpreterState): InterpreterContext[Unit] = {
    StateT.set[InterpreterErrorHandler, InterpreterState](newState)
  }

  def setStateAndContinue(newState: InterpreterState): InterpreterContext[Option[Boolean]] = {
    StateT.set[InterpreterErrorHandler, InterpreterState](newState).flatMap(_ => continue)
  }

  def abort(error: InterpreterError): InterpreterContext[Option[Boolean]] = StateT.liftF {
    Left(error).asInstanceOf[InterpreterErrorHandler[Option[Boolean]]]
  }

  def tailRecMAbort(error: InterpreterError): InterpreterContext[Either[Option[Boolean], Option[Boolean]]] = StateT.liftF {
    Left(error).asInstanceOf[InterpreterErrorHandler[Either[Option[Boolean], Option[Boolean]]]]
  }

  def tailRecMEvaluated(maybeValue: Option[Boolean]): InterpreterContext[Either[Option[Boolean], Option[Boolean]]] = {
    StateT.pure(Right(maybeValue))
  }

  def tailRecMEvaluated(value: Boolean): InterpreterContext[Either[Option[Boolean], Option[Boolean]]] = {
    StateT.pure(Right(Some(value)))
  }

  // Steps: How many steps should the interpreter execute
  def create(verbose: Boolean = false, maybeSteps: Option[Int] = None): InterpreterContext[Option[Boolean]] = {
    for {
      _ <- checkInvalidOpCode()
      _ <- checkDisabledOpCode()
      _ <- checkMaxPushSize()
      _ <- checkMaxScriptSize()
      _ <- checkIsPushOnly()

      result <- interpretScript(verbose, maybeSteps)
    } yield result
  }

  private def interpretScript(verbose: Boolean, maybeSteps: Option[Int]): InterpreterContext[Option[Boolean]] = {
    tailRecM((None: Option[Boolean], maybeSteps)) {
      case (Some(value), maybeNewSteps @ _) =>
        tailRecMEvaluated(Some(value)).map(_.leftMap { (_, maybeNewSteps) })

      case (None, Some(newSteps)) if newSteps <= 0 =>
        tailRecMEvaluated(None).map(_.leftMap { (_, Some(newSteps)) })

      case (None, maybeNewSteps) =>
        for {
          state <- getState
          _ <- checkOpCodeCount()
          _ <- checkMaxStackSize()
          result <- interpretOneOp(state, verbose, maybeNewSteps)
        } yield {
          result match {
            case (value, maybeUpdatedSteps) =>
              value.leftMap((_, maybeUpdatedSteps))
          }
        }
    }
  }

  import scala.language.existentials
  private def interpretOneOp(state: InterpreterState, verbose: Boolean, steps: Option[Int]) = {
    val result = state.currentScript match {
      case opCode :: tail =>
        val updatedContext = opCode match {
          case op: ArithmeticOp =>
            op.interpret(verbose)
          case op: BitwiseLogicOp =>
            op.interpret(verbose)
          case op: ConstantOp =>
            op.interpret(verbose)
          case op: CryptoOp =>
            op.interpret(verbose)
          case op: FlowControlOp =>
            op.interpret(verbose)
          case op: LocktimeOp =>
            op.interpret(verbose)
          case op: PseudoOp =>
            op.interpret(verbose)
          case op: ReservedOp =>
            op.interpret(verbose)
          case op: SpliceOp =>
            op.interpret(verbose)
          case op: StackOp =>
            op.interpret(verbose)
          case _ =>
            abort(GeneralError(OP_UNKNOWN, state))
        }

        for {
          _ <- setState(state.copy(currentScript = tail))
          result <- updatedContext
        } yield {
          result match {
            case Some(value) =>
              Right(Some(value))
            case None =>
              Left(None)
          }
        }

      case Nil =>
        state.scriptExecutionStage match {
          case ExecutingScriptSig =>
            for {
              _ <- setState(
                state.copy(
                  currentScript = state.scriptPubKey,
                  altStack = Seq.empty,
                  opCount = 0,
                  scriptExecutionStage = ExecutingScriptPubKey
                )
              )
              _ <- checkInvalidOpCode()
              _ <- checkDisabledOpCode()
              _ <- checkMaxPushSize()
              _ <- checkMaxScriptSize()
            } yield {
              Left(None)
            }

          case ExecutingScriptPubKey =>
            state.stack match {
              case Nil =>
                tailRecMEvaluated(false)

              case head :: tail =>
                maybeExecuteWitnessProgram(Some(state.scriptPubKey), state) match {
                  case Some(nextState) =>
                    nextState.map(_ => Left(None))

                  case None =>
                    if (state.ScriptFlags.p2sh() && isP2SHScript(state.scriptPubKey)) {
                      getSerializedScript(state.scriptSig) match {
                        case Some(serializedScript) =>
                          val payToScript = Parser.parse(serializedScript.bytes)
                          for {
                            _ <- setState(
                              state.copy(
                                currentScript = payToScript,
                                stack = tail,
                                altStack = Seq.empty,
                                opCount = 0,
                                scriptP2sh = Some(payToScript),
                                scriptExecutionStage = ExecutingScriptP2SH
                              )
                            )
                            _ <- checkInvalidOpCode()
                            _ <- checkDisabledOpCode()
                            _ <- checkMaxPushSize()
                            _ <- checkMaxScriptSize()
                          } yield {
                            Left(None)
                          }
                        case None =>
                          tailRecMAbort(NoSerializedP2SHScriptFound(OP_HASH160, state))
                      }
                    } else if (state.ScriptFlags.p2sh() && state.ScriptFlags.witness()) {
                      if (state.scriptWitnessStack.isEmpty) {
                        tailRecMEvaluated(head.bytes.toBoolean())
                      } else {
                        tailRecMAbort(WitnessProgramUnexpected(OP_UNKNOWN, state))
                      }
                    } else {
                      if (state.ScriptFlags.requireCleanStack()) {
                        tailRecMAbort(RequireCleanStack(OP_UNKNOWN, state))
                      } else {
                        tailRecMEvaluated(head.bytes.toBoolean())
                      }
                    }
                }
            }

          case ExecutingScriptP2SH =>
            state.stack match {
              case Nil =>
                tailRecMEvaluated(false)

              case head :: tail =>
                maybeExecuteWitnessProgram(state.scriptP2sh, state) match {
                  case Some(nextState) =>
                    nextState.map(_ => Left(None))

                  case None =>
                    if (state.ScriptFlags.requireCleanStack() && tail.nonEmpty) {
                      tailRecMAbort(RequireCleanStack(OP_UNKNOWN, state))
                    } else {
                      tailRecMEvaluated(head.bytes.toBoolean())
                    }
                }
            }

          case ExecutingScriptP2SH | ExecutingScriptWitness =>
            state.stack match {
              case Nil =>
                tailRecMEvaluated(false)

              case head :: tail =>
                if (state.ScriptFlags.requireCleanStack() && tail.nonEmpty) {
                  tailRecMAbort(RequireCleanStack(OP_UNKNOWN, state))
                } else {
                  tailRecMEvaluated(head.bytes.toBoolean())
                }
            }
        }
    }

    // Update the steps
    result.map(v => (v, steps.map(_ - 1)))
  }

  private def checkOpCodeCount(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      (state.opCount > MAX_OPCODES)
        .option(abort(ExceedMaxOpsCount(OP_UNKNOWN, state)))
        .getOrElse(continue)
    }
  }

  // NOTE: Rule 6) in https://github.com/bitcoin/bips/blob/master/bip-0062.mediawiki
  private def checkWitnessSuperfluousScriptSigOperation(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      val superfluousScriptSigOperation = state.ScriptFlags.witness() && state.stack.length > 2

      if (superfluousScriptSigOperation) {
        abort(WitnessMalleatedP2SH(OP_UNKNOWN, state))
      } else {
        continue
      }
    }
  }

  private def checkWitnessP2WPKHScriptSigEmpty(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      val p2wpkh = (state.scriptExecutionStage == ExecutingScriptPubKey) && state.ScriptFlags.witness()
      val malleated = p2wpkh && !state.scriptSig.isEmpty

      if (malleated) {
        abort(WitnessMalleated(OP_UNKNOWN, state))
      } else {
        continue
      }
    }
  }

  private def checkInvalidOpCode(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      state.currentScript.find(OpCodes.invalid.contains) match {
        case Some(opCode: ScriptOpCode) =>
          abort(BadOpCode(opCode, state, "Opcode not allowed"))

        case _ =>
          continue
      }
    }
  }

  private def checkDisabledOpCode(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      state.currentScript.find(OpCodes.disabled.contains) match {
        case Some(opCode: ScriptOpCode) =>
          abort(OpcodeDisabled(opCode, state))
        case _ =>
          continue
      }
    }
  }

  private def checkMaxPushSize(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      state.currentScript.filter(_.isInstanceOf[ScriptConstant]).filter(_.bytes.length > MAX_PUSH_SIZE) match {
        case Nil =>
          continue

        case _ =>
          abort(ExceedMaxPushSize(OP_UNKNOWN, state))
      }
    }
  }

  private def checkMaxStackSize(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      val totalStackSize = (state.stack ++ state.altStack).length
      if (totalStackSize > MAX_STACK_SIZE) {
        abort(ExceedMaxStackSize(OP_UNKNOWN, state))
      } else {
        continue
      }
    }
  }

  private def checkMaxScriptSize(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      val numberOfPushData1 = state.currentScript.filter(_ == OP_PUSHDATA1).length
      val numberOfPushData2 = state.currentScript.filter(_ == OP_PUSHDATA2).length
      val numberOfPushData4 = state.currentScript.filter(_ == OP_PUSHDATA4).length
      val skippedDataLengthBytes = numberOfPushData1 + numberOfPushData2 * 2 + numberOfPushData4 * 4

      val totalBytes = state.currentScript.map(_.bytes.length).sum + skippedDataLengthBytes

      if (totalBytes > MAX_SCRIPT_SIZE) {
        abort(ExceedMaxScriptSize(OP_UNKNOWN, state))
      } else {
        continue
      }
    }
  }

  private def checkIsPushOnly(): InterpreterContext[Option[Boolean]] = {
    getState.flatMap { state =>
      val pushOnlyEnabled = state.flags.contains(ScriptFlag.SCRIPT_VERIFY_SIGPUSHONLY)
      val shouldExecuteP2sh = isP2SHScript(state.scriptPubKey) && state.ScriptFlags.p2sh()
      val shouldCheckPushOnly = pushOnlyEnabled || shouldExecuteP2sh

      if (shouldCheckPushOnly && !isPushOnly(state.scriptSig)) {
        abort(ScriptSigPushOnly(OP_UNKNOWN, state))
      } else {
        continue
      }
    }
  }

  private def isP2SHScript(scriptPubkey: Seq[ScriptElement]): Boolean = {
    scriptPubkey.headOption.exists(_ == CryptoOp.OP_HASH160) &&
    scriptPubkey.tail.headOption.exists(_ == ConstantOp.OP_PUSHDATA(20)) &&
    scriptPubkey.lastOption.exists(_ == BitwiseLogicOp.OP_EQUAL)
  }

  private def rebuildScriptPubkeyAndStackFromWitness(witnessHash: ScriptConstant, witnessStack: Seq[ScriptElement]) = {
    witnessStack match {
      case Nil =>
        Left(WitnessRebuiltError.WitnessStackEmpty)

      case head :: tail =>
        witnessHash.bytes.length match {
          case 20 =>
            // P2WPKH
            val witnessProgramMatch = (Hash.Hash160(head.bytes.toArray).toSeq == witnessHash.bytes)
            if (witnessProgramMatch) {
              val witnessProgram = OP_DUP :: OP_HASH160 :: OP_PUSHDATA(20) :: witnessHash :: OP_EQUALVERIFY :: OP_CHECKSIG :: Nil
              Right((witnessProgram, witnessStack))
            } else {
              Left(WitnessRebuiltError.WitnessProgramMismatch)
            }

          case 32 =>
            val witnessProgramMatch = (Hash.Sha256(head.bytes.toArray).toSeq == witnessHash.bytes)
            if (witnessProgramMatch) {
              val witnessProgram = Parser.parse(head.bytes)
              Right((witnessProgram, removePushOps(tail)))
            } else {
              Left(WitnessRebuiltError.WitnessProgramMismatch)
            }

          case _ =>
            Left(WitnessRebuiltError.WitnessHashWrongLength)
        }
    }
  }

  private def getWitnessScript(
    scriptPubkey: Seq[ScriptElement],
    state: InterpreterState
  ): Either[WitnessRebuiltError, (ConstantOp, ScriptConstant)] = {
    import ConstantOp._

    val possibleVersionNumbers = Seq(
      OP_0,
      OP_FALSE,
      OP_1,
      OP_TRUE,
      OP_2,
      OP_3,
      OP_4,
      OP_5,
      OP_6,
      OP_7,
      OP_8,
      OP_9,
      OP_10,
      OP_11,
      OP_12,
      OP_13,
      OP_14,
      OP_15,
      OP_16
    )

    scriptPubkey match {
      case (version: ConstantOp) :: (_: OP_PUSHDATA) :: (scriptConstant: ScriptConstant) :: Nil =>
        val scriptLength = scriptConstant.bytes.length
        val isWitnessScript = possibleVersionNumbers.contains(version) && (scriptLength >= 2 && scriptLength <= 40)

        if (isWitnessScript) {
          if (version != OP_0 && state.flags.contains(ScriptFlag.SCRIPT_VERIFY_DISCOURAGE_UPGRADABLE_WITNESS_PROGRAM)) {
            Left(WitnessRebuiltError.WitnessProgramUpgradableVersion)
          } else {
            Right((version, scriptConstant))
          }
        } else {
          Left(WitnessRebuiltError.WitnessScriptUnableToExtract)
        }

      case _ =>
        Left(WitnessRebuiltError.WitnessScriptUnableToExtract)
    }
  }

  private def maybeExecuteWitnessProgram(
    maybeScript: Option[Seq[ScriptElement]],
    state: InterpreterState
  ): Option[InterpreterContext[Option[Boolean]]] = {

    state.ScriptFlags.witness().flatOption(maybeScript).flatMap { script =>
      tryRebuildScriptPubkeyAndStackFromWitness(script, state) match {
        case Right((rebuiltScript, rebuiltStack)) =>
          val interpreterContext = for {
            _ <- checkWitnessP2WPKHScriptSigEmpty()
            _ <- checkWitnessSuperfluousScriptSigOperation()
            _ <- setState(
              state.copy(
                currentScript = rebuiltScript,
                stack = rebuiltStack,
                altStack = Seq.empty,
                opCount = 0,
                scriptWitness = Some(rebuiltScript),
                sigVersion = SigVersion.SIGVERSION_WITNESS_V0,
                scriptExecutionStage = ExecutingScriptWitness
              )
            )
            _ <- checkInvalidOpCode()
            _ <- checkDisabledOpCode()
            _ <- checkMaxPushSize()
            result <- checkMaxScriptSize()
          } yield {
            result
          }

          Some(interpreterContext)

        case Left(WitnessRebuiltError.WitnessProgramMismatch) =>
          Some(abort(WitnessProgramMismatch(OP_UNKNOWN, state)))

        case Left(WitnessRebuiltError.WitnessScriptUnableToExtract) =>
          None

        case Left(WitnessRebuiltError.WitnessHashWrongLength) =>
          Some(abort(WitnessProgramWrongLength(OP_UNKNOWN, state)))

        case Left(WitnessRebuiltError.WitnessStackEmpty) =>
          Some(abort(WitnessProgramWitnessEmpty(OP_UNKNOWN, state)))

        case Left(WitnessRebuiltError.WitnessProgramUpgradableVersion) =>
          Some(abort(DiscourageUpgradableWitnessProgram(OP_UNKNOWN, state)))

        case Left(error @ _) =>
          Some(abort(GeneralError(OP_UNKNOWN, state)))
      }
    }
  }

  sealed trait WitnessRebuiltError

  object WitnessRebuiltError {
    case object WitnessScriptUnableToExtract extends WitnessRebuiltError
    case object WitnessProgramMismatch extends WitnessRebuiltError
    case object WitnessStackEmpty extends WitnessRebuiltError
    case object WitnessHashWrongLength extends WitnessRebuiltError
    case object WitnessProgramUpgradableVersion extends WitnessRebuiltError
  }

  private def tryRebuildScriptPubkeyAndStackFromWitness(
    script: Seq[ScriptElement],
    state: InterpreterState
  ): Either[WitnessRebuiltError, (Seq[ScriptElement], Seq[ScriptElement])] = {
    getWitnessScript(script, state).flatMap {
      case (version @ _, witnessHash) =>
        rebuildScriptPubkeyAndStackFromWitness(witnessHash, state.scriptWitnessStack.getOrElse(Seq.empty))
    }
  }

  private def isPushOnly(script: Seq[ScriptElement]): Boolean = {
    script.filterNot(_.isInstanceOf[ScriptConstant]).forall(ConstantOp.all.contains)
  }

  private def getSerializedScript(scriptSig: Seq[ScriptElement]): Option[ScriptElement] = {
    val fromLastPushOp = scriptSig.reverse.dropWhile { element =>
      element.isInstanceOf[OP_PUSHDATA] ||
      element == OP_PUSHDATA1 ||
      element == OP_PUSHDATA2 ||
      element == OP_PUSHDATA4
    }

    fromLastPushOp.headOption
  }

  private def removePushOps(script: Seq[ScriptElement]): Seq[ScriptElement] = {
    script.filterNot { element =>
      element.isInstanceOf[OP_PUSHDATA] ||
      element == OP_PUSHDATA1 ||
      element == OP_PUSHDATA2 ||
      element == OP_PUSHDATA4
    }
  }
}
