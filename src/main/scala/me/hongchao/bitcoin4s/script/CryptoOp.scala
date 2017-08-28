package me.hongchao.bitcoin4s.script

sealed trait CryptoOp extends OpCode

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

object CryptoOps {
  val all = Seq(
    OP_RIPEMD160, OP_SHA1, OP_SHA256, OP_HASH160, OP_HASH256, OP_CODESEPARATOR,
    OP_CHECKSIG, OP_CHECKSIGVERIFY, OP_CHECKMULTISIG, OP_CHECKMULTISIGVERIFY
  )
}