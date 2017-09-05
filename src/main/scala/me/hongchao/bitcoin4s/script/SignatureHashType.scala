package me.hongchao.bitcoin4s.script

sealed trait SignatureHashType extends Product {
  val value: Int
  val name = productPrefix
}

object SignatureHashType {
  case object SIGHASH_ALL extends SignatureHashType { val value = 1 }
  case object SIGHASH_NONE extends SignatureHashType { val value = 2 }
  case object SIGHASH_SINGLE extends SignatureHashType { val value = 3 }
  case object SIGHASH_ANYONECANPAY extends SignatureHashType { val value = 0x80 }
}