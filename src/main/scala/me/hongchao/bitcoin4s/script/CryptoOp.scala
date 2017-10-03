package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.crypto.Hash._
import me.hongchao.bitcoin4s.crypto.{PublicKey, Signature}
import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.FlowControlOp.OP_VERIFY
import me.hongchao.bitcoin4s.script.InterpreterError.NotEnoughElementsInStack
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
                    script = state.script,
                    stack = checkResult.option(ScriptNum(1)).getOrElse(ScriptNum(0)) +: tail,
                    opCount = state.opCount + 1
                  )
                ).flatMap(continue)

              case _ =>
                abort(NotEnoughElementsInStack(opCode, state.stack))
            }
          }

        case OP_CHECKSIGVERIFY =>
          getState
            .flatMap { state =>
              setState(
                state.copy(
                  script = OP_CHECKSIG +: OP_VERIFY +: state.script,
                  opCount = state.opCount - 1
                )
              )
            }
            .flatMap(continue)

        case OP_CHECKMULTISIG =>
          getState.flatMap { state =>
            val maybeSplitStack = for {
              (ms, rest0)         <- state.stack.splitAtOpt(1)
              numberOfPubKeys     <- ms.headOption.flatMap { m =>
                val numOfPubKeys = ScriptNum.toLong(m.bytes).toInt
                (numOfPubKeys >= 0 && numOfPubKeys <= 20).option(numOfPubKeys)
              }
              (pubKeys, rest1)    <- rest0.splitAtOpt(numberOfPubKeys)
              (ns, rest2)         <- rest1.splitAtOpt(1)
              numberOfSignatures  <- ns.headOption.flatMap { n =>
                val numOfSignatures = ScriptNum.toLong(n.bytes).toInt
                (numOfSignatures >= 0 && numberOfPubKeys <= numberOfPubKeys).option(numOfSignatures)
              }
              (signatures, rest)  <- rest2.splitAtOpt(numberOfSignatures)
            } yield (pubKeys, signatures, rest)

            maybeSplitStack match {
              case Some((pubKeys, signatures, rest)) =>
                val checkResult = checkSignatures(pubKeys, signatures, state)

                val oneMorePop = rest.tail // Due to the bug in the reference client

                setState(
                  state.copy(
                    stack = checkResult.option(ScriptNum(1)).getOrElse(ScriptNum(0)) +: oneMorePop,
                    opCount = state.opCount + 1 + pubKeys.length
                  )
                ).flatMap(continue)
              case None =>
                abort(NotEnoughElementsInStack(opCode, state.stack))
            }
          }

        case OP_CHECKMULTISIGVERIFY =>
          getState
            .flatMap { state =>
              setState(
                state.copy(
                  script = OP_CHECKMULTISIG +: OP_VERIFY +: state.script,
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
          abort(NotEnoughElementsInStack(opCode, state.stack))
      }

      getState.flatMap(hashTopElement)
    }
  }

  @tailrec
  def checkSignatures(encodedPublicKeys: Seq[ScriptElement], encodedSignatures: Seq[ScriptElement], state: InterpreterState): Boolean = {
    encodedSignatures match {
      case encodedSignature :: tail =>
        val maybeEncodedPubKeyWithSigature = encodedPublicKeys.find { encodedPubKey =>
          checkSignature(encodedPubKey, encodedSignature, state)
        }

        maybeEncodedPubKeyWithSigature match {
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
    } yield {
      val sigHashType = SignatureHashType(sigHashFlagBytes.headOption.map(_ & 0xff).getOrElse(1))

      // FIXME: better error handling
      if (state.flags.contains(ScriptFlag.SCRIPT_VERIFY_STRICTENC) && !sigHashType.isValid()) {
        throw new RuntimeException(s"Invalid sigHashType: $sigHashType")
      }

      val hashedTransaction = state.transaction.signingHash(
        pubKeyScript = state.scriptPubKey,
        inputIndex = state.inputIndex,
        sigHashType = sigHashType,
        sigVersion = SIGVERSION_BASE
      )
      pubKey.verify(hashedTransaction, signature)
    }).exists(identity)
  }
}
