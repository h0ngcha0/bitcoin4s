package me.hongchao.bitcoin4s.script

import cats.Eval
import cats.data.{State, StateT}
import me.hongchao.bitcoin4s.crypto.Hash._
import me.hongchao.bitcoin4s.crypto.{PublicKey, Signature}
import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.FlowControlOp.OP_VERIFY
import me.hongchao.bitcoin4s.script.InterpreterError.{NotEnoughElementsInStack, NotImplemented}
import me.hongchao.bitcoin4s.script.TransactionOps._
import me.hongchao.bitcoin4s.script.Interpreter._
import me.hongchao.bitcoin4s.script.SigVersion.SIGVERSION_BASE

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
    def interpret(opCode: CryptoOp): InterpreterContext = {
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
          State.get.flatMap { state =>
            state.stack match {
              case encodedPublicKey :: encodedSignature :: tail =>
                val checkResult = (for {
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

                State.set(
                  state.copy(
                    script = state.script,
                    stack = ScriptConstant(checkResult.option(Seq(1.toByte)).getOrElse(Seq(0.toByte))) +: tail,
                    opCount = state.opCount + 1
                  )
                ).flatMap(continue)

              case _ =>
                abort(NotEnoughElementsInStack(opCode, state.stack))
            }
          }

        case OP_CHECKSIGVERIFY =>
          State.get[InterpreterState]
            .flatMap { state =>
              State.set(
                state.copy(
                  script = OP_CHECKSIG +: OP_VERIFY +: state.script.tail,
                  opCount = state.opCount - 1
                )
              )
            }
            .flatMap(continue)

        case OP_CHECKMULTISIG =>
          notImplemented(opCode)

        case OP_CHECKMULTISIGVERIFY =>
          notImplemented(opCode)
      }
    }

    private def onOpHash(opCode: ScriptOpCode, hash: (Array[Byte]) => Array[Byte]): InterpreterContext = {
      def hashTopElement(state: InterpreterState): InterpreterContext = state.stack match {
        case head :: _ =>
          val hashed = hash(head.bytes.toArray)
          State
            .set(state.replaceStackTopElement(ScriptConstant(hashed)))
            .flatMap(continue)
        case _ =>
          abort(NotEnoughElementsInStack(opCode, state.stack))
      }

      State.get.flatMap(hashTopElement)
    }

    private def notImplemented(opCode: ScriptOpCode): State[InterpreterState, InterpreterResult] = {
      State.get.flatMap { state =>
        abort(NotImplemented(opCode, state.stack))
      }
    }
  }
}
