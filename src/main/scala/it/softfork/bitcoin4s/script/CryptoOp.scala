package it.softfork.bitcoin4s.script

import it.softfork.bitcoin4s.crypto.Hash._
import it.softfork.bitcoin4s.crypto.{PublicKey, Secp256k1, Signature}
import it.softfork.bitcoin4s.Utils._
import it.softfork.bitcoin4s.script.FlowControlOp.OP_VERIFY
import it.softfork.bitcoin4s.script.InterpreterError._
import it.softfork.bitcoin4s.script.RichTransaction._
import it.softfork.bitcoin4s.script.Interpreter._
import tech.minna.utilities.TraitEnumeration
import scala.annotation.tailrec
import cats.implicits._
import it.softfork.bitcoin4s.crypto.PublicKey.DecodeResult
import it.softfork.bitcoin4s.crypto.Signature.{ECDSASignature, EmptySignature}
import it.softfork.bitcoin4s.script.OpCodes.OP_UNKNOWN
import it.softfork.bitcoin4s.script.SigVersion.{SIGVERSION_BASE, SIGVERSION_WITNESS_V0}

import scala.util.{Failure, Success, Try}

sealed trait CryptoOp extends ScriptOpCode

object CryptoOp {
  case object OP_RIPEMD160 extends CryptoOp { val value = 166 }
  case object OP_SHA1 extends CryptoOp { val value = 167 }
  case object OP_SHA256 extends CryptoOp { val value = 168 }
  case object OP_HASH160 extends CryptoOp { val value = 169 }
  case object OP_HASH256 extends CryptoOp { val value = 170 }
  case object OP_CODESEPARATOR extends CryptoOp { val value = 171 }
  case object OP_CHECKSIG extends CryptoOp { val value = 172 }
  case object OP_CHECKSIGVERIFY extends CryptoOp { val value = 173 }
  case object OP_CHECKMULTISIG extends CryptoOp { val value = 174 }
  case object OP_CHECKMULTISIGVERIFY extends CryptoOp { val value = 175 }

  val all = TraitEnumeration.values[CryptoOp]

  sealed trait MultiSigCheckError extends Product
  object MultiSigCheckError {
    case object NotEnoughElements extends MultiSigCheckError
    case object WrongNumberOfPubKeys extends MultiSigCheckError
    case object WrongNumberOfSignatures extends MultiSigCheckError
    case object PubKeysNumberWrongEncoding extends MultiSigCheckError
    case object SignatureNumberWrongEncoding extends MultiSigCheckError
  }

  implicit val interpreter = new InterpretableOp[CryptoOp] {
    def interpret(opCode: CryptoOp): InterpreterContext[Option[Boolean]] = {
      opCode match {
        case OP_RIPEMD160 =>
          onOpHash(opCode, RipeMD160.apply _)

        case OP_SHA1 =>
          onOpHash(opCode, Sha1.apply _)

        case OP_SHA256 =>
          onOpHash(opCode, Sha256.apply _)

        case OP_HASH160 =>
          onOpHash(opCode, Hash160.apply _)

        case OP_HASH256 =>
          onOpHash(opCode, Hash256.apply _)

        case OP_CODESEPARATOR =>
          continue

        case OP_CHECKSIG =>
          getState.flatMap { state =>
            state.stack match {
              case encodedPublicKey :: encodedSignature :: tail =>
                def handleResult(result: Boolean) = {
                  if (state.flags.contains(ScriptFlag.SCRIPT_VERIFY_NULLFAIL) && !result & encodedSignature.bytes.nonEmpty) {
                    abort(SignatureVerificationNullFail(opCode, state))
                  } else {
                    setStateAndContinue(
                      state.copy(
                        stack = result.option(ScriptNum(1)).getOrElse(ScriptNum(0)) +: tail,
                        opCount = state.opCount + 1
                      )
                    )
                  }
                }

                import PublicKey._
                Signature.decode(encodedSignature.bytes) match {
                  case Some((signature, sigHashFlagBytes)) =>
                    signature match {
                      case ECDSASignature(_, s) if s.compareTo(Secp256k1.halfCurveOrder) > 0 && state.flags.contains(ScriptFlag.SCRIPT_VERIFY_LOW_S) =>
                        abort(SignatureHighS(opCode, state))

                      case _ =>
                        if (checkSignatureEncoding(encodedSignature.bytes, state.flags)) {
                          PublicKey.decode(encodedPublicKey.bytes, state.ScriptFlags.strictEncoding) match {
                            case DecodeResult.Ok(decodedPublicKey) =>
                              val notCompressed = !decodedPublicKey.compressed
                              val executingP2WSH = state.scriptExecutionStage == ScriptExecutionStage.ExecutingScriptWitness

                              if (executingP2WSH && notCompressed && state.flags.contains(ScriptFlag.SCRIPT_VERIFY_WITNESS_PUBKEYTYPE)) {
                                abort(WitnessPubkeyUncompressed(opCode, state))
                              } else {
                                Try {
                                  checkSignature(decodedPublicKey, signature, sigHashFlagBytes, state)
                                } match {
                                  case Success(result) =>
                                    handleResult(result)

                                  case Failure(_: InvalidSigHashType) =>
                                    abort(InvalidSigHashType(opCode, state))

                                  case Failure(e) =>
                                    throw e
                                }
                              }

                            case DecodeResult.OkButNotStrictEncoded(decodedPublicKey@_) =>
                              abort(PublicKeyWrongEncoding(opCode, state))

                            case DecodeResult.Failure =>
                              handleResult(false)
                          }
                        } else {
                          abort(SignatureWrongEncoding(OP_CHECKSIG, state))
                        }
                    }

                  case None =>
                    if (state.ScriptFlags.strictEncoding() || state.flags.contains(ScriptFlag.SCRIPT_VERIFY_DERSIG)) {
                      abort(SignatureWrongEncoding(OP_CHECKSIG, state))
                    } else {
                      handleResult(false)
                    }
                }

              case _ =>
                abort(InvalidStackOperation(opCode, state))
            }
          }

        case OP_CHECKSIGVERIFY =>
          getState
            .flatMap { state =>
              setStateAndContinue(
                state.copy(
                  currentScript = OP_CHECKSIG +: OP_VERIFY +: state.currentScript,
                  opCount = state.opCount - 1
                )
              )
            }

        case OP_CHECKMULTISIG =>
          getState.flatMap { state =>
            import MultiSigCheckError._

            val maybeSplitStack = for {
              v1                   <- state.stack.splitAtEither(1, NotEnoughElements)
              (ms, rest0)          = v1
              numberOfPubKeys      <- ms.headEither(NotEnoughElements).flatMap { m =>
                Try {
                  ScriptNum(m.bytes, state.ScriptFlags.requireMinimalEncoding).value.toInt
                } match {
                  case Success(numOfPubKeys) =>
                    (numOfPubKeys >= 0 && numOfPubKeys <= 20)
                      .option(Right(numOfPubKeys))
                      .getOrElse(Left(WrongNumberOfPubKeys))

                  case Failure(_) =>
                    Left(PubKeysNumberWrongEncoding)
                }
              }
              v2                   <- rest0.splitAtEither(numberOfPubKeys, NotEnoughElements)
              (pubKeys, rest1)     = v2
              v3                   <- rest1.splitAtEither(1, NotEnoughElements)
              (ns, rest2)          = v3
              numberOfSignatures   <- ns.headEither(NotEnoughElements).flatMap { n =>
                Try {
                  ScriptNum(n.bytes, state.ScriptFlags.requireMinimalEncoding).value.toInt
                } match {
                  case Success(numOfSignatures) =>
                    (numOfSignatures >= 0 && numOfSignatures <= numberOfPubKeys)
                      .option(Right(numOfSignatures))
                      .getOrElse(Left(WrongNumberOfSignatures))

                  case Failure(_) =>
                    Left(SignatureNumberWrongEncoding)
                }
              }
              v4                   <- rest2.splitAtEither(numberOfSignatures, NotEnoughElements)
              (signatures, rest)   = v4
            } yield (pubKeys, signatures, rest)

            maybeSplitStack match {
              case Right((pubKeys, signatures, rest)) =>
                val nonEmptyEncodedPubKeys = pubKeys.filter(_ != ConstantOp.OP_0)

                Try {
                  println(s"i m here")
                  checkSignatures(nonEmptyEncodedPubKeys, signatures, state)
                } match {
                  case Success(checkResult) =>
                  println(s"i m here 2")
                    if (state.flags.contains(ScriptFlag.SCRIPT_VERIFY_NULLFAIL) && !checkResult & signatures.exists(_.bytes.nonEmpty)) {
                      abort(SignatureVerificationNullFail(OP_CHECKMULTISIG, state))
                    } else {
                      println(s"i m here 3")
                      // NOTE: Popping extra element due to the bug in the reference client
                      rest match {
                        case head :: tail =>
                          println(s"i m here 4")
                          if (state.flags.contains(ScriptFlag.SCRIPT_VERIFY_NULLDUMMY) && head.bytes.nonEmpty) {
                            // Reference: https://github.com/bitcoin/bips/blob/master/bip-0147.mediawiki
                            abort(MultiSigNullDummy(opCode, state))
                          } else {
                            println(s"i m here 5")
                            println(checkResult)
                            setStateAndContinue(
                              state.copy(
                                stack = checkResult.option(ScriptNum(1)).getOrElse(ScriptNum(0)) +: tail,
                                opCount = state.opCount + 1 + nonEmptyEncodedPubKeys.length
                              )
                            )
                          }
                        case Nil =>
                          println(s"i m here 6")
                          abort(InvalidStackOperation(opCode, state))
                      }
                    }

                  case Failure(err: PublicKeyWrongEncoding) =>
                    abort(err)

                  case Failure(err: SignatureWrongEncoding) =>
                    abort(err)

                  case Failure(err: SignatureVerificationNullFail) =>
                    abort(err)

                  case Failure(err: WitnessPubkeyUncompressed) =>
                    abort(err)

                  case Failure(e) =>
                    throw e
                }

              case Left(WrongNumberOfPubKeys) =>
                abort(WrongPubKeyCount(opCode, state))

              case Left(WrongNumberOfSignatures) =>
                abort(WrongSignaturesCount(opCode, state))

              case Left(PubKeysNumberWrongEncoding) =>
                abort(GeneralError(opCode, state))

              case Left(SignatureNumberWrongEncoding) =>
                abort(GeneralError(opCode, state))

              case Left(_) =>
                abort(InvalidStackOperation(opCode, state))
            }
          }

        case OP_CHECKMULTISIGVERIFY =>
          getState
            .flatMap { state =>
              setStateAndContinue(
                state.copy(
                  currentScript = OP_CHECKMULTISIG +: OP_VERIFY +: state.currentScript,
                  opCount = state.opCount - 1
                )
              )
            }
      }
    }

    private def onOpHash(opCode: ScriptOpCode, hash: (Array[Byte]) => Array[Byte]): InterpreterContext[Option[Boolean]] = {
      def hashTopElement(state: InterpreterState): InterpreterContext[Option[Boolean]] = state.stack match {
        case head :: tail =>
          val hashed = hash(head.bytes.toArray)
          setStateAndContinue(
            state.copy(
              stack = ScriptConstant(hashed) +: tail,
              opCount = state.opCount + 1
            )
          )
        case _ =>
          abort(InvalidStackOperation(opCode, state))
      }

      getState.flatMap(hashTopElement)
    }
  }

  def checkSignature(encodedPublicKey: ScriptElement, encodedSignature: ScriptElement, state: InterpreterState, strictEnc: Boolean): Boolean = {
    Signature.decode(encodedSignature.bytes) match {
      case Some((signature, sigHashFlagBytes)) =>
        signature match {
          case ECDSASignature(_, s) if s.compareTo(Secp256k1.halfCurveOrder) > 0 && state.flags.contains(ScriptFlag.SCRIPT_VERIFY_LOW_S) =>
            throw SignatureHighS(OP_CHECKMULTISIG, state)

          case _ =>
            if (checkSignatureEncoding(encodedSignature.bytes, state.flags)) {
              PublicKey.decode(encodedPublicKey.bytes, strictEnc) match {
                case DecodeResult.Ok(decodedPublicKey) =>
                  val notCompressed = !decodedPublicKey.compressed
                  val executingP2WSH = state.scriptExecutionStage == ScriptExecutionStage.ExecutingScriptWitness

                  if (executingP2WSH && notCompressed && state.flags.contains(ScriptFlag.SCRIPT_VERIFY_WITNESS_PUBKEYTYPE)) {
                    throw WitnessPubkeyUncompressed(OP_CHECKMULTISIG, state)
                  } else {
                    checkSignature(decodedPublicKey, signature, sigHashFlagBytes, state)
                  }

                case _ =>
                  if (state.ScriptFlags.strictEncoding()) {
                    throw PublicKeyWrongEncoding(OP_CHECKMULTISIG, state)
                  }

                  false
              }
            } else {
              throw SignatureWrongEncoding(OP_CHECKMULTISIG, state)
            }
        }

      case None =>
        if (state.ScriptFlags.strictEncoding() || state.flags.contains(ScriptFlag.SCRIPT_VERIFY_DERSIG)) {
          throw SignatureWrongEncoding(OP_CHECKMULTISIG, state)
        }

        false
    }
  }

  @tailrec
  def checkSignatures(encodedPublicKeys: Seq[ScriptElement], encodedSignatures: Seq[ScriptElement], state: InterpreterState, strictEnc: Boolean = true): Boolean = {
    if (encodedSignatures.nonEmpty && encodedPublicKeys.isEmpty) {
      false
    } else {
      encodedSignatures match {
        case encodedSignature :: restOfEncodedSignatures =>
          val maybeEncodedPublicKeysForSignature = encodedPublicKeys.find { encodedPublicKey =>
            checkSignature(encodedPublicKey, encodedSignature, state, strictEnc)
          }

          maybeEncodedPublicKeysForSignature match {
            case Some(encodedPublicKeysForSignature) =>
              checkSignatures(encodedPublicKeys.filterNot(_ == encodedPublicKeysForSignature), restOfEncodedSignatures, state, strictEnc)
            case None =>
              false
          }

        case Nil =>
          true
      }
    }
  }

  def checkSignature(pubKey: PublicKey, signature: Signature, sigHashFlagBytes: Seq[Byte], state: InterpreterState): Boolean = {
    signature match {
      case EmptySignature =>
        false
      case ecdsaSignature: ECDSASignature =>
        val currentScript = getCurrentScript(state)
        val sigHashType = SignatureHashType(sigHashFlagBytes.headOption.map(_ & 0xff).getOrElse(1))

        if (state.flags.contains(ScriptFlag.SCRIPT_VERIFY_STRICTENC) && !sigHashType.isValid()) {
          throw InvalidSigHashType(OP_UNKNOWN, state)
        }

        val hashedTransaction = state.sigVersion match {
          case SIGVERSION_BASE =>
            state.transaction.signingHashPreSegwit(
              currentScript,
              inputIndex = state.inputIndex,
              sigHashType = sigHashType
            )

          case SIGVERSION_WITNESS_V0 =>
            state.transaction.signingHashSegwit(
              currentScript,
              inputIndex = state.inputIndex,
              amount = state.amount,
              sigHashType = sigHashType
            )
        }

        pubKey.verify(hashedTransaction, ecdsaSignature)
    }
  }

  private def getCurrentScript(state: InterpreterState): Seq[ScriptElement] = {
    state.scriptExecutionStage match {
      case ScriptExecutionStage.ExecutingScriptPubKey =>
        state.scriptPubKey
      case ScriptExecutionStage.ExecutingScriptSig =>
        state.scriptSig
      case ScriptExecutionStage.ExecutingScriptP2SH =>
        state.scriptP2sh.get // FIXME: get rid of get
      case ScriptExecutionStage.ExecutingScriptWitness =>
        state.scriptWitness.get
    }
  }

  // Check that an encoding is correct
  private def checkSignatureEncoding(signatureBytes: Seq[Byte], flags: Seq[ScriptFlag]): Boolean = {
    val notValidDerEncoded = !Signature.isValidSignatureEncoding(signatureBytes)
    val nonEmptySignature = signatureBytes.nonEmpty
    val derSigOrStrictEnc = Seq(ScriptFlag.SCRIPT_VERIFY_DERSIG, ScriptFlag.SCRIPT_VERIFY_STRICTENC).exists(flags.contains)

    !(nonEmptySignature && derSigOrStrictEnc && notValidDerEncoded)
  }
}
