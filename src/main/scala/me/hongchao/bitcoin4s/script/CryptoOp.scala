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
                val checkResult = checkSignature(encodedPublicKey, encodedSignature, state)

                setState(
                  state.copy(
                    currentScript = state.currentScript,
                    stack = checkResult.option(ScriptNum(1)).getOrElse(ScriptNum(0)) +: tail,
                    opCount = state.opCount + 1
                  )
                ).flatMap(continue)

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
                val nonEmptyPubKeys = pubKeys.filter(_ != ConstantOp.OP_0)
                val checkResult = checkSignatures(nonEmptyPubKeys, signatures, state)

                // NOTE: Due to the bug in the reference client
                if (rest.nonEmpty) {
                  val oneMorePop = rest.tail

                  setState(
                    state.copy(
                      stack = checkResult.option(ScriptNum(1)).getOrElse(ScriptNum(0)) +: oneMorePop,
                      opCount = state.opCount + 1 + nonEmptyPubKeys.length
                    )
                  ).flatMap(continue)
                } else {
                  abort(InvalidStackOperation(opCode, state))
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
  def checkSignatures(encodedPublicKeys: Seq[ScriptElement], encodedSignatures: Seq[ScriptElement], state: InterpreterState): Boolean = {
    encodedSignatures match {
      case encodedSignature :: tail =>
        val maybeEncodedPubKeyWithSignature = encodedPublicKeys.find { encodedPubKey =>
          checkSignature(encodedPubKey, encodedSignature, state)
        }

        maybeEncodedPubKeyWithSignature match {
          case Some(encodedPubKey) =>
            val newEncodedPublicKeys = encodedPublicKeys.takeWhile(_ != encodedPubKey) ++ encodedPublicKeys.dropWhile(_ != encodedPubKey).tail
            checkSignatures(newEncodedPublicKeys, tail, state)
          case None =>
            false
        }

      case Nil =>
        true
    }
  }

  def checkSignature(encodedPublicKey: ScriptElement, encodedSignature: ScriptElement, state: InterpreterState): Boolean = {
    (for {
      pubKey <- PublicKey.decode(encodedPublicKey.bytes)
      (signature, sigHashFlagBytes) <- Signature.decode(encodedSignature.bytes)
      currentScript <- getCurrentScript(state)
    } yield {
      val sigHashType = SignatureHashType(sigHashFlagBytes.headOption.map(_ & 0xff).getOrElse(1))

      // FIXME: better error handling
      if (state.flags.contains(ScriptFlag.SCRIPT_VERIFY_STRICTENC) && !sigHashType.isValid()) {
        throw new RuntimeException(s"Invalid sigHashType: $sigHashType")
      }

      val hashedTransaction = state.transaction.signingHash(
        currentScript = currentScript,
        inputIndex = state.inputIndex,
        sigHashType = sigHashType,
        sigVersion = SIGVERSION_BASE
      )
      pubKey.verify(hashedTransaction, signature)
    }).exists(identity)
  }

  private def getCurrentScript(state: InterpreterState): Option[Seq[ScriptElement]] = {
    state.scriptExecutionStage match {
      case ScriptExecutionStage.ExecutingScriptPubKey =>
        Some(state.scriptPubKey)
      case ScriptExecutionStage.ExecutingScriptSig =>
        Some(state.scriptSig)
      case ScriptExecutionStage.ExecutingScriptP2SH =>
        state.p2shScript
    }
  }
}
