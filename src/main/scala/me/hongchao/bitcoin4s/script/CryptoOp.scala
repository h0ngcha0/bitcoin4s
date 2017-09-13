package me.hongchao.bitcoin4s.script

import me.hongchao.bitcoin4s.crypto.Hash._
import me.hongchao.bitcoin4s.crypto.{PublicKey, Signature}
import me.hongchao.bitcoin4s.Utils._
import me.hongchao.bitcoin4s.script.FlowControlOp.OP_VERIFY
import me.hongchao.bitcoin4s.script.InterpreterError.{NotEnoughElementsInStack, NotImplemented}
import me.hongchao.bitcoin4s.script.TransactionOps._

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

  implicit val interpreter = new Interpreter[CryptoOp] {
    def interpret(opCode: CryptoOp, context: InterpreterContext): InterpreterContext = {
      val stack = context.stack
      val transaction = context.transaction

      opCode match {
        case OP_RIPEMD160 =>
          onOpHash(opCode, RipeMD160.apply _, context)

        case OP_SHA1 =>
          onOpHash(opCode, Sha1.apply _, context)

        case OP_SHA256 =>
          onOpHash(opCode, Sha256.apply _, context)

        case OP_HASH160 =>
          onOpHash(opCode, Hash160.apply _, context)

        case OP_HASH256 =>
          onOpHash(opCode, Hash256.apply _, context)

        case OP_CODESEPARATOR =>
          throw NotImplemented(opCode, stack)

        case OP_CHECKSIG =>
          stack match {
            case encodedPublicKey :: encodedSignature :: tail =>
              val pubKey = PublicKey.decode(encodedPublicKey.bytes)
              val signature = Signature(encodedSignature.bytes)

              val hashedTransaction = transaction.hash(
                pubKeyScript = context.scriptPubKey,
                inputIndex = context.inputIndex,
                sigHashType = SignatureHashType(1),
                sigVersion = SIGVERSION_BASE
              )

              val checkResult = pubKey.verify(hashedTransaction, signature)

              context.copy(
                script = context.script.tail,
                stack = ScriptConstant(checkResult.option(Seq(1.toByte)).getOrElse(Seq(0.toByte))) +: tail,
                opCount = context.opCount + 1
              )

            case _ =>
              throw NotEnoughElementsInStack(opCode, stack)
          }

        case OP_CHECKSIGVERIFY =>
          context.copy(
            script = OP_CHECKSIG +: OP_VERIFY +: context.script.tail,
            opCount = context.opCount - 1
          )

        case OP_CHECKMULTISIG =>
          throw NotImplemented(opCode, stack)

        case OP_CHECKMULTISIGVERIFY =>
          throw NotImplemented(opCode, stack)
      }
    }

    private def onOpHash(opCode: ScriptOpCode, hash: (Array[Byte]) => Array[Byte], context: InterpreterContext): InterpreterContext = {
      context.stack match {
        case head :: _ =>
          val hashed = hash(head.bytes.toArray)
          context.replaceStackTopElement(ScriptConstant(hashed))
        case _ =>
          throw NotEnoughElementsInStack(opCode, context.stack)
      }
    }
  }
}
