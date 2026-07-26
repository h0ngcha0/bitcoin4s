package it.softfork.bitcoin4s.script

import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq
import scala.util.{Failure, Success, Try}

import it.softfork.bitcoin4s.crypto.{Hash, Schnorr, Secp256k1}
import it.softfork.bitcoin4s.script.ArithmeticOp.OP_NUMEQUAL
import it.softfork.bitcoin4s.script.BitwiseLogicOp.OP_EQUAL
import it.softfork.bitcoin4s.script.ConstantOp._
import it.softfork.bitcoin4s.script.CryptoOp._
import it.softfork.bitcoin4s.script.FlowControlOp._
import it.softfork.bitcoin4s.script.InterpretableOp.ops._
import it.softfork.bitcoin4s.script.InterpreterError._
import it.softfork.bitcoin4s.script.OpCodes.OP_UNKNOWN
import it.softfork.bitcoin4s.script.ScriptExecutionStage.ExecutingScriptWitness
import it.softfork.bitcoin4s.script.SigVersion.{SIGVERSION_TAPROOT, SIGVERSION_TAPSCRIPT}
import it.softfork.bitcoin4s.transaction.{OutPoint, Script, TxOut}
import it.softfork.bitcoin4s.transaction.structure.VarInt
import it.softfork.bitcoin4s.utils._

// BIP341 and BIP342 validation for native witness-v1 32-byte programs.
//scalastyle:off magic.number method.length cyclomatic.complexity number.of.methods
object Taproot {
  private val TapLeafVersion = 0xc0
  private val ControlBaseSize = 33
  private val ControlNodeSize = 32
  private val ControlMaxSize = ControlBaseSize + 128 * ControlNodeSize
  private val ValidationWeightOffset = 50
  private val ValidationWeightPerSignature = 50
  private val NoCodeSeparator = 0xffffffffL

  private val opSuccess = Set(80, 98) ++
    (126 to 129) ++
    (131 to 134) ++
    (137 to 138) ++
    (141 to 142) ++
    (149 to 153) ++
    (187 to 254)

  private val disabled = Set(126, 127, 128, 129, 131, 132, 133, 134, 141, 142, 149, 150, 151, 152, 153)

  private case class Instruction(code: Int, data: Option[Seq[Byte]], position: Long)

  def execute(program: Seq[Byte], state: InterpreterState): Either[InterpreterError, InterpreterState] = {
    val originalWitness = state.scriptWitnessStack.getOrElse(Seq.empty)

    originalWitness match {
      case Nil =>
        Left(WitnessProgramWitnessEmpty(OP_UNKNOWN, state))

      case _ =>
        val annexPresent =
          originalWitness.length >= 2 && originalWitness.head.bytes.headOption.contains(0x50.toByte)
        val annex = annexPresent.option(originalWitness.head.bytes)
        val witness = annexPresent.option(originalWitness.tail).getOrElse(originalWitness)

        witness match {
          case signature :: Nil =>
            checkSchnorrSignature(
              signature.bytes,
              program,
              state,
              SIGVERSION_TAPROOT,
              annex,
              None,
              NoCodeSeparator
            ).map { _ =>
              successfulState(state, SIGVERSION_TAPROOT)
            }

          case control :: script :: stack =>
            validateControlBlock(control.bytes, script.bytes, program, state).flatMap {
              case (leafVersion, tapLeafHash) if leafVersion == TapLeafVersion =>
                executeTapscript(
                  script.bytes,
                  stack,
                  originalWitness,
                  annex,
                  tapLeafHash.toIndexedSeq,
                  state
                )

              case _ if state.flags.contains(
                    ScriptFlag.SCRIPT_VERIFY_DISCOURAGE_UPGRADABLE_TAPROOT_VERSION
                  ) =>
                Left(DiscourageUpgradableTaprootVersion(OP_UNKNOWN, state))

              case _ =>
                Right(successfulState(state, SIGVERSION_TAPSCRIPT))
            }

          case _ =>
            Left(WitnessProgramWitnessEmpty(OP_UNKNOWN, state))
        }
    }
  }

  def tapLeafHash(leafVersion: Int, script: Seq[Byte]): Array[Byte] = {
    Hash.TaggedSha256(
      "TapLeaf",
      Array(leafVersion.toByte) ++ compactSize(script.length.toLong) ++ script
    )
  }

  def outputKey(
    internalPublicKey: Seq[Byte],
    merkleRoot: Seq[Byte]
  ): Option[(Seq[Byte], Boolean)] = {
    for {
      point <- Schnorr.liftX(internalPublicKey)
      tweak = Schnorr.unsigned(
        Hash.TaggedSha256("TapTweak", (internalPublicKey ++ merkleRoot).toArray)
          .toIndexedSeq
      )
      if tweak.compareTo(Secp256k1.params.getN) < 0
      output = point.add(Secp256k1.params.getG.multiply(tweak)).normalize()
      if !output.isInfinity
    } yield {
      val encoded = output.getEncoded(true)
      (ArraySeq.unsafeWrapArray(encoded.tail), encoded.head == 0x03.toByte)
    }
  }

  private def successfulState(state: InterpreterState, sigVersion: SigVersion): InterpreterState = {
    state.copy(
      currentScript = Seq.empty,
      scriptWitness = Some(Seq.empty),
      stack = Seq(ScriptNum(1)),
      altStack = Seq.empty,
      sigVersion = sigVersion,
      scriptExecutionStage = ExecutingScriptWitness
    )
  }

  private def validateControlBlock(
    control: Seq[Byte],
    script: Seq[Byte],
    program: Seq[Byte],
    state: InterpreterState
  ): Either[InterpreterError, (Int, Array[Byte])] = {
    val validSize =
      control.length >= ControlBaseSize &&
        control.length <= ControlMaxSize &&
        (control.length - ControlBaseSize) % ControlNodeSize == 0

    if (!validSize) {
      Left(TaprootWrongControlSize(OP_UNKNOWN, state))
    } else {
      val leafVersion = control.head & 0xfe
      val leafHash = tapLeafHash(leafVersion, script)
      val merkleRoot = control
        .drop(ControlBaseSize)
        .grouped(ControlNodeSize)
        .foldLeft(leafHash) { (current, node) =>
          val (first, second) = if (lexicographicallyLess(current.toIndexedSeq, node)) {
            (current.toIndexedSeq, node)
          } else {
            (node, current.toIndexedSeq)
          }
          Hash.TaggedSha256("TapBranch", (first ++ second).toArray)
        }
      val expectedParity = (control.head & 1) == 1
      val commitmentMatches =
        outputKey(control.slice(1, ControlBaseSize), merkleRoot.toIndexedSeq)
        .exists { case (output, parity) =>
          output == program && parity == expectedParity
        }

      if (commitmentMatches) {
        Right((leafVersion, leafHash))
      } else {
        Left(WitnessProgramMismatch(OP_UNKNOWN, state))
      }
    }
  }

  private def executeTapscript(
    script: Seq[Byte],
    initialStack: Seq[ScriptElement],
    originalWitness: Seq[ScriptElement],
    annex: Option[Seq[Byte]],
    tapLeafHash: Seq[Byte],
    state: InterpreterState
  ): Either[InterpreterError, InterpreterState] = {
    parseTapscript(script, state).flatMap {
      case None =>
        Right(successfulState(state, SIGVERSION_TAPSCRIPT))

      case Some(instructions) =>
        val tapscriptState = state.copy(
          currentScript = Seq.empty,
          scriptWitness = Some(Parser.parse(script)),
          stack = initialStack,
          altStack = Seq.empty,
          sigVersion = SIGVERSION_TAPSCRIPT,
          scriptExecutionStage = ExecutingScriptWitness
        )

        if (initialStack.length > Interpreter.MAX_STACK_SIZE) {
          Left(ExceedMaxStackSize(OP_UNKNOWN, tapscriptState))
        } else if (initialStack.exists(_.bytes.length > Interpreter.MAX_PUSH_SIZE)) {
          Left(ExceedMaxPushSize(OP_UNKNOWN, tapscriptState))
        } else {
          val validationWeight =
            serializedWitnessSize(originalWitness) + ValidationWeightOffset
          evaluateInstructions(
            instructions,
            tapscriptState,
            annex,
            tapLeafHash,
            validationWeight
          )
        }
    }
  }

  private def parseTapscript(
    script: Seq[Byte],
    state: InterpreterState
  ): Either[InterpreterError, Option[Vector[Instruction]]] = {
    val bytes = script.toArray

    def parse(offset: Int, position: Long, result: Vector[Instruction])
      : Either[InterpreterError, Option[Vector[Instruction]]] = {
      if (offset >= bytes.length) {
        Right(Some(result))
      } else {
        val code = bytes(offset) & 0xff
        if (opSuccess.contains(code)) {
          if (state.flags.contains(ScriptFlag.SCRIPT_VERIFY_DISCOURAGE_OP_SUCCESS)) {
            Left(DiscourageOpSuccess(OP_UNKNOWN, state))
          } else {
            Right(None)
          }
        } else {
          val lengthBytes = if (code <= 75) {
            0
          } else if (code == 76) {
            1
          } else if (code == 77) {
            2
          } else if (code == 78) {
            4
          } else {
            -1
          }

          if (lengthBytes < 0) {
            parse(offset + 1, position + 1, result :+ Instruction(code, None, position))
          } else if (offset + 1 + lengthBytes > bytes.length) {
            Left(BadOpCode(OP_UNKNOWN, state, "Malformed tapscript push"))
          } else {
            val dataLength = if (code <= 75) {
              code.toLong
            } else {
              readLittleEndian(bytes, offset + 1, lengthBytes)
            }
            val dataOffset = offset + 1 + lengthBytes
            val remaining = bytes.length.toLong - dataOffset

            if (dataLength > remaining || dataLength > Int.MaxValue) {
              Left(BadOpCode(OP_UNKNOWN, state, "Malformed tapscript push"))
            } else {
              val nextOffset = dataOffset + dataLength.toInt
              val data = ArraySeq.unsafeWrapArray(bytes.slice(dataOffset, nextOffset))
              parse(
                nextOffset,
                position + 1,
                result :+ Instruction(code, Some(data), position)
              )
            }
          }
        }
      }
    }

    parse(0, 0, Vector.empty)
  }

  private def evaluateInstructions(
    instructions: Vector[Instruction],
    initialState: InterpreterState,
    annex: Option[Seq[Byte]],
    tapLeafHash: Seq[Byte],
    initialValidationWeight: Int
  ): Either[InterpreterError, InterpreterState] = {

    @tailrec
    def loop(
      index: Int,
      state: InterpreterState,
      conditions: Vector[Boolean],
      codeSeparatorPosition: Long,
      validationWeight: Int
    ): Either[InterpreterError, InterpreterState] = {
      if (index >= instructions.length) {
        if (conditions.nonEmpty) {
          Left(UnbalancedConditional(OP_UNKNOWN, state))
        } else {
          Right(state.copy(currentScript = Seq.empty, opCount = 0))
        }
      } else {
        val instruction = instructions(index)
        val executing = conditions.forall(identity)
        val pushTooLarge = instruction.data.exists(_.length > Interpreter.MAX_PUSH_SIZE)

        if (pushTooLarge) {
          Left(ExceedMaxPushSize(OP_UNKNOWN, state))
        } else if (disabled.contains(instruction.code)) {
          Left(OpcodeDisabled(opCode(instruction.code), state))
        } else if (instruction.code == 101 || instruction.code == 102) {
          Left(BadOpCode(opCode(instruction.code), state, "Invalid opcode"))
        } else {
          val evaluated = instruction.data match {
            case Some(data) if executing =>
              if (
                state.flags.contains(ScriptFlag.SCRIPT_VERIFY_MINIMALDATA) &&
                !isMinimalPush(instruction.code, data)
              ) {
                Left(NotMinimalEncoding(opCode(instruction.code), state))
              } else {
                Right((state.copy(stack = ScriptConstant(data.toArray) +: state.stack), conditions,
                  codeSeparatorPosition, validationWeight))
              }

            case Some(_) =>
              Right((state, conditions, codeSeparatorPosition, validationWeight))

            case None if instruction.code == OP_IF.value || instruction.code == OP_NOTIF.value =>
              if (executing) {
                state.stack match {
                  case value :: tail =>
                    val minimal = value.bytes.isEmpty || value.bytes == Seq(1.toByte)
                    if (!minimal) {
                      Left(TapscriptMinimalIf(opCode(instruction.code), state))
                    } else {
                      val truthy = value.bytes.toBoolean()
                      val selected =
                        if (instruction.code == OP_NOTIF.value) !truthy else truthy
                      Right((state.copy(stack = tail), conditions :+ selected,
                        codeSeparatorPosition, validationWeight))
                    }

                  case Nil =>
                    Left(InvalidStackOperation(opCode(instruction.code), state))
                }
              } else {
                Right((state, conditions :+ false, codeSeparatorPosition, validationWeight))
              }

            case None if instruction.code == OP_ELSE.value =>
              conditions.lastOption match {
                case Some(last) =>
                  Right((state, conditions.dropRight(1) :+ !last, codeSeparatorPosition,
                    validationWeight))
                case None =>
                  Left(UnbalancedConditional(opCode(instruction.code), state))
              }

            case None if instruction.code == OP_ENDIF.value =>
              conditions.lastOption match {
                case Some(_) =>
                  Right((state, conditions.dropRight(1), codeSeparatorPosition, validationWeight))
                case None =>
                  Left(UnbalancedConditional(opCode(instruction.code), state))
              }

            case None if !executing =>
              Right((state, conditions, codeSeparatorPosition, validationWeight))

            case None if instruction.code == OP_CODESEPARATOR.value =>
              Right((state, conditions, instruction.position, validationWeight))

            case None
                if instruction.code == OP_CHECKSIG.value ||
                  instruction.code == OP_CHECKSIGVERIFY.value =>
              evaluateCheckSig(
                instruction.code,
                state,
                annex,
                tapLeafHash,
                codeSeparatorPosition,
                validationWeight
              ).map { case (newState, newWeight) =>
                (newState, conditions, codeSeparatorPosition, newWeight)
              }

            case None if instruction.code == OP_CHECKSIGADD.value =>
              evaluateCheckSigAdd(
                state,
                annex,
                tapLeafHash,
                codeSeparatorPosition,
                validationWeight
              ).map { case (newState, newWeight) =>
                (newState, conditions, codeSeparatorPosition, newWeight)
              }

            case None
                if instruction.code == OP_CHECKMULTISIG.value ||
                  instruction.code == OP_CHECKMULTISIGVERIFY.value =>
              Left(TapscriptCheckMultiSig(opCode(instruction.code), state))

            case None if instruction.code == BitwiseLogicOp.OP_EQUALVERIFY.value =>
              evaluateThenVerify(OP_EQUAL, state).map { newState =>
                (newState, conditions, codeSeparatorPosition, validationWeight)
              }

            case None if instruction.code == ArithmeticOp.OP_NUMEQUALVERIFY.value =>
              evaluateThenVerify(OP_NUMEQUAL, state).map { newState =>
                (newState, conditions, codeSeparatorPosition, validationWeight)
              }

            case None =>
              evaluateExistingOpcode(opCode(instruction.code), state).map { newState =>
                (newState, conditions, codeSeparatorPosition, validationWeight)
              }
          }

          evaluated match {
            case Left(error) =>
              Left(error)
            case Right((newState, _, _, _))
                if newState.stack.length + newState.altStack.length > Interpreter.MAX_STACK_SIZE =>
              Left(ExceedMaxStackSize(opCode(instruction.code), newState))
            case Right((newState, newConditions, newCodeSeparator, newWeight)) =>
              loop(index + 1, newState, newConditions, newCodeSeparator, newWeight)
          }
        }
      }
    }

    loop(0, initialState, Vector.empty, NoCodeSeparator, initialValidationWeight)
  }

  private def evaluateCheckSig(
    code: Int,
    state: InterpreterState,
    annex: Option[Seq[Byte]],
    tapLeafHash: Seq[Byte],
    codeSeparatorPosition: Long,
    validationWeight: Int
  ): Either[InterpreterError, (InterpreterState, Int)] = {
    state.stack match {
      case publicKey :: signature :: tail =>
        checkTapscriptSignature(
          signature.bytes,
          publicKey.bytes,
          state,
          annex,
          tapLeafHash,
          codeSeparatorPosition,
          validationWeight
        ).flatMap { case (valid, newWeight) =>
          if (code == OP_CHECKSIGVERIFY.value) {
            if (valid) {
              Right((state.copy(stack = tail), newWeight))
            } else {
              Left(VerificationFailed(OP_CHECKSIGVERIFY, state))
            }
          } else {
            val result = if (valid) ScriptNum(1) else ScriptNum(0)
            Right((state.copy(stack = result +: tail), newWeight))
          }
        }

      case _ =>
        Left(InvalidStackOperation(opCode(code), state))
    }
  }

  private def evaluateCheckSigAdd(
    state: InterpreterState,
    annex: Option[Seq[Byte]],
    tapLeafHash: Seq[Byte],
    codeSeparatorPosition: Long,
    validationWeight: Int
  ): Either[InterpreterError, (InterpreterState, Int)] = {
    state.stack match {
      case publicKey :: number :: signature :: tail =>
        Try {
          ScriptNum(number.bytes, state.ScriptFlags.requireMinimalEncoding()).value
        } match {
          case Success(value) =>
            checkTapscriptSignature(
              signature.bytes,
              publicKey.bytes,
              state,
              annex,
              tapLeafHash,
              codeSeparatorPosition,
              validationWeight
            ).map { case (valid, newWeight) =>
              val result = ScriptNum(value + (if (valid) 1 else 0))
              (state.copy(stack = result +: tail), newWeight)
            }

          case Failure(_) =>
            Left(GeneralError(OP_CHECKSIGADD, state))
        }

      case _ =>
        Left(InvalidStackOperation(OP_CHECKSIGADD, state))
    }
  }

  private def checkTapscriptSignature(
    signature: Seq[Byte],
    publicKey: Seq[Byte],
    state: InterpreterState,
    annex: Option[Seq[Byte]],
    tapLeafHash: Seq[Byte],
    codeSeparatorPosition: Long,
    validationWeight: Int
  ): Either[InterpreterError, (Boolean, Int)] = {
    val signaturePresent = signature.nonEmpty
    val newWeight = if (signaturePresent) {
      validationWeight - ValidationWeightPerSignature
    } else {
      validationWeight
    }

    if (newWeight < 0) {
      Left(TapscriptValidationWeight(OP_CHECKSIG, state))
    } else if (publicKey.isEmpty) {
      Left(TapscriptEmptyPublicKey(OP_CHECKSIG, state))
    } else if (publicKey.length == 32) {
      if (!signaturePresent) {
        Right((false, newWeight))
      } else {
        checkSchnorrSignature(
          signature,
          publicKey,
          state,
          SIGVERSION_TAPSCRIPT,
          annex,
          Some(tapLeafHash),
          codeSeparatorPosition
        ).map(_ => (true, newWeight))
      }
    } else if (
      state.flags.contains(ScriptFlag.SCRIPT_VERIFY_DISCOURAGE_UPGRADABLE_PUBKEYTYPE)
    ) {
      Left(DiscourageUpgradablePublicKeyType(OP_CHECKSIG, state))
    } else {
      Right((signaturePresent, newWeight))
    }
  }

  private def checkSchnorrSignature(
    signature: Seq[Byte],
    publicKey: Seq[Byte],
    state: InterpreterState,
    sigVersion: SigVersion,
    annex: Option[Seq[Byte]],
    tapLeafHash: Option[Seq[Byte]],
    codeSeparatorPosition: Long
  ): Either[InterpreterError, Unit] = {
    val parsed = signature.length match {
      case 64 =>
        Right((signature, 0))
      case 65 if signature.last != 0 =>
        Right((signature.dropRight(1), signature.last & 0xff))
      case 65 =>
        Left(SchnorrSignatureHashType(OP_CHECKSIG, state))
      case _ =>
        Left(SchnorrSignatureWrongSize(OP_CHECKSIG, state))
    }

    parsed.flatMap { case (rawSignature, hashType) =>
      signatureHash(
        state,
        hashType,
        sigVersion,
        annex,
        tapLeafHash,
        codeSeparatorPosition
      ).flatMap { message =>
        if (Schnorr.verify(publicKey, message.toIndexedSeq, rawSignature)) {
          Right(())
        } else {
          Left(SchnorrSignatureVerificationFailed(OP_CHECKSIG, state))
        }
      }
    }
  }

  private[script] def signatureHash(
    state: InterpreterState,
    hashType: Int,
    sigVersion: SigVersion,
    annex: Option[Seq[Byte]],
    tapLeafHash: Option[Seq[Byte]],
    codeSeparatorPosition: Long
  ): Either[InterpreterError, Array[Byte]] = {
    val validHashType =
      hashType == 0 || hashType == 1 || hashType == 2 || hashType == 3 ||
        hashType == 0x81 || hashType == 0x82 || hashType == 0x83
    val transaction = state.transaction
    val inputIndex = state.inputIndex
    val outputType = if (hashType == 0) 1 else hashType & 3
    val anyoneCanPay = (hashType & 0x80) != 0
    val contextAvailable =
      state.spentOutputs.length == transaction.tx_in.length &&
        inputIndex >= 0 &&
        inputIndex < transaction.tx_in.length
    val singleOutputAvailable = outputType != 3 || inputIndex < transaction.tx_out.length

    if (!validHashType || !contextAvailable || !singleOutputAvailable) {
      Left(SchnorrSignatureHashType(OP_CHECKSIG, state))
    } else {
      val message = Array.newBuilder[Byte]
      message += hashType.toByte
      message ++= uint32ToBytes(transaction.version)
      message ++= uint32ToBytes(transaction.lock_time)

      if (!anyoneCanPay) {
        message ++= sha256(transaction.tx_in.flatMap { input =>
          encodeOutPoint(input.previous_output)
        }.toArray)
        message ++= sha256(state.spentOutputs.flatMap(output => uInt64ToBytes(output.value)).toArray)
        message ++= sha256(state.spentOutputs.flatMap(output => encodeScript(output.pk_script)).toArray)
        message ++= sha256(transaction.tx_in.flatMap(input => uint32ToBytes(input.sequence)).toArray)
      }

      if (outputType == 1) {
        message ++= sha256(transaction.tx_out.flatMap(encodeOutput).toArray)
      }

      val extFlag = if (sigVersion == SIGVERSION_TAPSCRIPT) 1 else 0
      message += ((extFlag * 2) + annex.fold(0)(_ => 1)).toByte

      if (anyoneCanPay) {
        val input = transaction.tx_in(inputIndex)
        message ++= encodeOutPoint(input.previous_output)
        message ++= encodeOutput(state.spentOutputs(inputIndex))
        message ++= uint32ToBytes(input.sequence)
      } else {
        message ++= uint32ToBytes(inputIndex.toLong)
      }

      annex.foreach { value =>
        message ++= sha256(encodeScript(Script(value)))
      }

      if (outputType == 3) {
        message ++= sha256(encodeOutput(transaction.tx_out(inputIndex)))
      }

      if (sigVersion == SIGVERSION_TAPSCRIPT) {
        message ++= tapLeafHash.getOrElse(Seq.empty)
        message += 0.toByte
        message ++= uint32ToBytes(codeSeparatorPosition)
      }

      Right(Hash.TaggedSha256("TapSighash", Array(0.toByte) ++ message.result()))
    }
  }

  private def evaluateThenVerify(
    opCode: ScriptOpCode,
    state: InterpreterState
  ): Either[InterpreterError, InterpreterState] = {
    evaluateExistingOpcode(opCode, state).flatMap { updated =>
      updated.stack match {
        case value :: tail if value.bytes.toBoolean() =>
          Right(updated.copy(stack = tail))
        case _ :: _ =>
          Left(VerificationFailed(opCode, updated))
        case Nil =>
          Left(InvalidStackOperation(opCode, updated))
      }
    }
  }

  private def evaluateExistingOpcode(
    opCode: ScriptOpCode,
    state: InterpreterState
  ): Either[InterpreterError, InterpreterState] = {
    val context = opCode match {
      case op: ArithmeticOp   => op.interpret(false)
      case op: BitwiseLogicOp => op.interpret(false)
      case op: ConstantOp     => op.interpret(false)
      case op: CryptoOp       => op.interpret(false)
      case op: FlowControlOp  => op.interpret(false)
      case op: LocktimeOp     => op.interpret(false)
      case op: PseudoOp       => op.interpret(false)
      case op: ReservedOp     => op.interpret(false)
      case op: SpliceOp       => op.interpret(false)
      case op: StackOp        => op.interpret(false)
      case _                  => Interpreter.abort(BadOpCode(opCode, state, "Invalid opcode"))
    }

    context.run(state.copy(currentScript = Seq.empty)).map(_._1)
  }

  private def opCode(value: Int): ScriptOpCode = {
    OpCodes.all.find(_.value == value).getOrElse(OP_UNKNOWN)
  }

  private[script] def serializedWitnessSize(witness: Seq[ScriptElement]): Int = {
    compactSize(witness.length.toLong).length +
      witness
        .map(element => compactSize(element.bytes.length.toLong).length + element.bytes.length)
        .sum
  }

  private def compactSize(value: Long): Array[Byte] = {
    VarInt.varIntCodec.encode(value).toBytes
  }

  private def encodeOutPoint(outPoint: OutPoint): Array[Byte] = {
    OutPoint.codec.encode(outPoint).toBytes
  }

  private def encodeOutput(output: TxOut): Array[Byte] = {
    TxOut.codec.encode(output).toBytes
  }

  private def encodeScript(script: Script): Array[Byte] = {
    Script.codec.encode(script).toBytes
  }

  private def sha256(bytes: Array[Byte]): Array[Byte] = Hash.Sha256(bytes)

  private def readLittleEndian(bytes: Array[Byte], offset: Int, length: Int): Long = {
    (0 until length).foldLeft(0L) { (result, index) =>
      result | ((bytes(offset + index).toLong & 0xffL) << (8 * index))
    }
  }

  private def isMinimalPush(code: Int, data: Seq[Byte]): Boolean = {
    data.length match {
      case 0 =>
        code == 0
      case 1 if data.head >= 1 && data.head <= 16 =>
        code == 0x50 + data.head
      case 1 if data.head == 0x81.toByte =>
        code == OP_1NEGATE.value
      case size if size <= 75 =>
        code == size
      case size if size <= 255 =>
        code == OP_PUSHDATA1.value
      case size if size <= 65535 =>
        code == OP_PUSHDATA2.value
      case _ =>
        code == OP_PUSHDATA4.value
    }
  }

  private def lexicographicallyLess(first: Seq[Byte], second: Seq[Byte]): Boolean = {
    first
      .zip(second)
      .collectFirst {
        case (left, right) if left != right =>
          (left & 0xff) < (right & 0xff)
      }
      .getOrElse(first.length < second.length)
  }
}
//scalastyle:on magic.number method.length cyclomatic.complexity number.of.methods
