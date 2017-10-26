package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.crypto.Hash._
import me.hongchao.bitcoin4s.crypto.{PublicKey, Signature}
import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.FlowControlOp.OP_VERIFY
import me.hongchao.bitcoin4s.script.InterpreterError._
import me.hongchao.bitcoin4s.script.TransactionOps._
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.SigVersion.SIGVERSION_BASE

import scala.annotation.tailrec
import cats.implicits._
import me.hongchao.bitcoin4s.crypto.PublicKey.DecodeResult
import me.hongchao.bitcoin4s.crypto.Signature.{ECDSASignature, EmptySignature}
import me.hongchao.bitcoin4s.script.OpCodes.OP_UNKNOWN

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

  val all = Seq(
    OP_RIPEMD160, OP_SHA1, OP_SHA256, OP_HASH160, OP_HASH256, OP_CODESEPARATOR,
    OP_CHECKSIG, OP_CHECKSIGVERIFY, OP_CHECKMULTISIG, OP_CHECKMULTISIGVERIFY
  )

  sealed trait MultiSigCheckError extends Product
  object MultiSigCheckError {
    case object NotEnoughElements extends MultiSigCheckError
    case object WrongNumberOfPubKeys extends MultiSigCheckError
    case object WrongNumberOfSignatures extends MultiSigCheckError
  }

  implicit val interpreter = new Interpretable[CryptoOp] {
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
          continue(OP_CODESEPARATOR)

        case OP_CHECKSIG =>
          getState.flatMap { state =>
            state.stack match {
              case encodedPublicKey :: encodedSignature :: tail =>
                def handleResult(result: Boolean) = {
                  if (state.ScriptFlags.nullfail() && !result & encodedSignature.bytes.nonEmpty) {
                    abort(SignatureVerificationNullFail(opCode, state))
                  } else {
                    setState(
                      state.copy(
                        stack = result.option(ScriptNum(1)).getOrElse(ScriptNum(0)) +: tail,
                        opCount = state.opCount + 1
                      )
                    ).flatMap(continue)
                  }
                }

                import PublicKey._
                Signature.decode(encodedSignature.bytes) match {
                  case Some((signature, sigHashFlagBytes)) =>
                    if (checkSignatureEncoding(encodedSignature.bytes, state.flags)) {
                      PublicKey.decode(encodedPublicKey.bytes, state.ScriptFlags.strictEncoding) match {
                        case DecodeResult.Ok(decodedPublicKey) =>
                          val checkResult = checkSignature(decodedPublicKey, signature, sigHashFlagBytes, state)
                          handleResult(checkResult)
                        case DecodeResult.OkButNotStrictEncoded(decodedPublicKey) =>
                          abort(PublicKeyWrongEncoding(opCode, state))
                        case DecodeResult.Failure =>
                          handleResult(false)
                      }
                    } else {
                      abort(SignatureWrongEncoding(OP_CHECKSIG, state))
                    }
                  case None =>
                    if (state.ScriptFlags.strictEncoding() || state.ScriptFlags.derSig()) {
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
              setState(
                state.copy(
                  currentScript = OP_CHECKSIG +: OP_VERIFY +: state.currentScript,
                  opCount = state.opCount - 1
                )
              )
            }
            .flatMap(continue)

        case OP_CHECKMULTISIG =>
          getState.flatMap { state =>
            import MultiSigCheckError._

            val maybeSplitStack = for {
              v1                   <- state.stack.splitAtEither(1, NotEnoughElements)
              val (ms, rest0)      = v1
              numberOfPubKeys      <- ms.headEither(NotEnoughElements).flatMap { m =>
                val numOfPubKeys = ScriptNum.toLong(m.bytes).toInt
                (numOfPubKeys >= 0 && numOfPubKeys <= 20)
                  .option(Right(numOfPubKeys))
                  .getOrElse(Left(WrongNumberOfPubKeys))
              }
              v2                   <- rest0.splitAtEither(numberOfPubKeys, NotEnoughElements)
              val (pubKeys, rest1) = v2
              v3                   <- rest1.splitAtEither(1, NotEnoughElements)
              val (ns, rest2)      = v3
              numberOfSignatures   <- ns.headEither(NotEnoughElements).flatMap { n =>
                val numOfSignatures = ScriptNum.toLong(n.bytes).toInt
                (numOfSignatures >= 0 && numOfSignatures <= numberOfPubKeys)
                  .option(Right(numOfSignatures))
                  .getOrElse(Left(WrongNumberOfSignatures))
              }
              v4                   <- rest2.splitAtEither(numberOfSignatures, NotEnoughElements)
              val (signatures, rest) = v4
            } yield (pubKeys, signatures, rest)


            maybeSplitStack match {
              case Right((pubKeys, signatures, rest)) =>
                val nonEmptyEncodedPubKeys = pubKeys.filter(_ != ConstantOp.OP_0)

                Try {
                  checkSignatures(nonEmptyEncodedPubKeys, signatures, state)
                } match {
                  case Success(checkResult) =>
                    if (state.ScriptFlags.nullfail() && !checkResult & signatures.exists(_.bytes.nonEmpty)) {
                      abort(SignatureVerificationNullFail(OP_CHECKMULTISIG, state))
                    } else {
                      // NOTE: Popping extra element due to the bug in the reference client
                      rest match {
                        case head :: tail =>
                          if (state.ScriptFlags.nulldummy() && head.bytes.nonEmpty) {
                            // Reference: https://github.com/bitcoin/bips/blob/master/bip-0147.mediawiki
                            abort(MultiSigNullDummy(opCode, state))
                          } else {
                            setState(
                              state.copy(
                                stack = checkResult.option(ScriptNum(1)).getOrElse(ScriptNum(0)) +: tail,
                                opCount = state.opCount + 1 + nonEmptyEncodedPubKeys.length
                              )
                            ).flatMap(continue)
                          }
                        case Nil =>
                          abort(InvalidStackOperation(opCode, state))
                      }
                    }

                  case Failure(err: PublicKeyWrongEncoding) =>
                    abort(err)

                  case Failure(err: SignatureWrongEncoding) =>
                    abort(err)

                  case Failure(err: SignatureVerificationNullFail) =>
                    abort(err)

                  case Failure(e) =>
                    throw e
                }

              case Left(WrongNumberOfPubKeys) =>
                abort(WrongPubKeyCount(opCode, state))

              case Left(WrongNumberOfSignatures) =>
                abort(WrongSignaturesCount(opCode, state))

              case Left(_) =>
                abort(InvalidStackOperation(opCode, state))
            }
          }

        case OP_CHECKMULTISIGVERIFY =>
          getState
            .flatMap { state =>
              setState(
                state.copy(
                  currentScript = OP_CHECKMULTISIG +: OP_VERIFY +: state.currentScript,
                  opCount = state.opCount - 1
                )
              )
            }
            .flatMap(continue)
      }
    }

    private def onOpHash(opCode: ScriptOpCode, hash: (Array[Byte]) => Array[Byte]): InterpreterContext[Option[Boolean]] = {
      def hashTopElement(state: InterpreterState): InterpreterContext[Option[Boolean]] = state.stack match {
        case head :: _ =>
          val hashed = hash(head.bytes.toArray)
          setState(state.replaceStackTopElement(ScriptConstant(hashed))).flatMap(continue)
        case _ =>
          abort(InvalidStackOperation(opCode, state))
      }

      getState.flatMap(hashTopElement)
    }
  }

  @tailrec
  def checkSignatures(encodedPublicKeys: Seq[ScriptElement], encodedSignatures: Seq[ScriptElement], state: InterpreterState, strictEnc: Boolean = true): Boolean = {
    encodedSignatures match {
      case encodedSignature :: tail =>
        val maybeEncodedPubKeyWithSignature = encodedPublicKeys.headOption.map { encodedPubKey =>
          Signature.decode(encodedSignature.bytes) match {
            case Some((signature, sigHashFlagBytes)) =>
              if (checkSignatureEncoding(encodedSignature.bytes, state.flags)) {
                PublicKey.decode(encodedPubKey.bytes, strictEnc) match {
                  case DecodeResult.Ok(decodedPubKey) =>
                    checkSignature(decodedPubKey, signature, sigHashFlagBytes, state)
                  case _ =>
                    if (state.ScriptFlags.strictEncoding()) {
                      throw PublicKeyWrongEncoding(OP_CHECKMULTISIG, state)
                    } else {
                      false
                    }
                }
              } else {
                throw SignatureWrongEncoding(OP_CHECKMULTISIG, state)
              }

            case None =>
              if (state.ScriptFlags.strictEncoding() || state.ScriptFlags.derSig()) {
                throw SignatureWrongEncoding(OP_CHECKMULTISIG, state)
              } else {
                false
              }
          }
        }

        maybeEncodedPubKeyWithSignature match {
          case Some(true) =>
            checkSignatures(encodedPublicKeys.tail, tail, state)
          case _ =>
            false
        }

      case Nil =>
        true
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

        val hashedTransaction = state.transaction.signingHash(
          currentScript = currentScript,
          inputIndex = state.inputIndex,
          sigHashType = sigHashType,
          sigVersion = SIGVERSION_BASE
        )

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
        state.p2shScript.get // FIXME: get rid of get
    }
  }

  // Check that an encoding is correct
  private def checkSignatureEncoding(signatureBytes: Seq[Byte], flags: Seq[ScriptFlag]) = {
    val notValidDerEncoded = !Signature.isValidSignatureEncoding(signatureBytes)
    val nonEmptySignature = signatureBytes.nonEmpty
    val derSigOrStrictEnc = Seq(ScriptFlag.SCRIPT_VERIFY_DERSIG, ScriptFlag.SCRIPT_VERIFY_STRICTENC).exists(flags.contains)

    !(nonEmptySignature && derSigOrStrictEnc && notValidDerEncoded)
  }
}
